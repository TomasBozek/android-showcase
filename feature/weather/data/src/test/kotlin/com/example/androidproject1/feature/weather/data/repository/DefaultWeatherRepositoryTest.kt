package com.example.androidproject1.feature.weather.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.feature.weather.data.database.WeatherDatabase
import com.example.androidproject1.feature.weather.data.source.DefaultLocalWeatherDataSource
import com.example.androidproject1.feature.weather.data.source.DefaultRemoteWeatherDataSource
import com.example.androidproject1.feature.weather.data.source.DefaultSuggestedPlacesDataSource
import com.example.androidproject1.feature.weather.domain.OpenMeteoConfig
import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.feature.weather.domain.WeatherCode
import com.example.androidproject1.service.core.domain.error.NotFoundError
import com.example.androidproject1.service.core.domain.error.ServerError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.domain.test.TestDispatchers
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.network.HttpClientFactory
import com.example.androidproject1.service.network.NetworkConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Two hosts on one `MockEngine` and a real SQLite: the search maps the geocoder's shape, a saved
 * place observes, a forecast is fetched once with the place's coordinates and read from the table
 * after, and a failing host over a cached forecast emits the forecast and then the failure.
 */
@RunWith(RobolectricTestRunner::class)
class DefaultWeatherRepositoryTest {

    private lateinit var database: WeatherDatabase

    private val requests = mutableListOf<String>()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WeatherDatabase::class.java,
        )
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    private fun repository(engine: MockEngine) = DefaultWeatherRepository(
        logger = FakeLogger(),
        localWeatherDataSource = DefaultLocalWeatherDataSource(database.weatherDao()),
        suggestedPlacesDataSource = DefaultSuggestedPlacesDataSource(),
        remoteWeatherDataSource = DefaultRemoteWeatherDataSource(
            client = HttpClientFactory.create(
                engine = engine,
                config = NetworkConfig(baseUrl = "https://fixtures.test/"),
            ),
            dispatcherProvider = TestDispatchers,
            config = CONFIG,
        ),
    )

    private fun openMeteo() = MockEngine { request ->
        requests += request.url.toString()
        val route = request.url.segments.filter { it.isNotEmpty() }
        when {
            request.url.host == "geocoding.test" && route == listOf("v1", "search") -> respondJson(
                if (request.url.parameters["name"] == "Pra") GEOCODING_JSON else "{}",
            )

            request.url.host == "forecast.test" && route == listOf("v1", "forecast") -> respondJson(FORECAST_JSON)

            // The geocoder by id answers the one place it is asked for, bare rather than wrapped.
            request.url.host == "geocoding.test" &&
                route == listOf("v1", "get") &&
                request.url.parameters["id"] == HAMBURG.id.toString() -> respondJson(HAMBURG_JSON)

            else -> respondError(HttpStatusCode.NotFound)
        }
    }

    private fun failing() = MockEngine {
        requests += it.url.toString()
        respondError(HttpStatusCode.ServiceUnavailable)
    }

    private fun MockRequestHandleScope.respondJson(json: String) = respond(
        content = json,
        status = HttpStatusCode.OK,
        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
    )

    @Test
    fun `a search maps the geocoder's answer, and no match is an empty list`() = runTest {
        val repository = repository(openMeteo())

        val found = (repository.searchPlaces("Pra") as Outcome.Success).data
        assertEquals(listOf("Praha"), found.map { it.name })
        assertEquals("a region equal to the name is dropped", null, found[0].region)
        assertEquals("Europe/Prague", found[0].timezone)

        assertEquals(emptyList<Place>(), (repository.searchPlaces("zzz") as Outcome.Success).data)
    }

    @Test
    fun `a saved place observes, and removing it observes too`() = runTest {
        val repository = repository(openMeteo())

        repository.savePlace(PRAHA)
        assertEquals(listOf(PRAHA), (repository.observeSavedPlaces().first() as Outcome.Success).data)

        repository.removePlace(PRAHA.id)
        assertEquals(emptyList<Place>(), (repository.observeSavedPlaces().first() as Outcome.Success).data)
    }

    @Test
    fun `movePlace reaches the DAO, and observeSavedPlaces reads the position order`() = runTest {
        val repository = repository(openMeteo())
        repository.savePlace(PRAHA)
        repository.savePlace(BRNO)
        repository.savePlace(OSTRAVA)

        assertEquals(
            "saved is appended, not newest first",
            listOf("Praha", "Brno", "Ostrava"),
            (repository.observeSavedPlaces().first() as Outcome.Success).data.map { it.name },
        )

        assertTrue(repository.movePlace(OSTRAVA.id, toIndex = 0) is Outcome.Success)

        assertEquals(
            listOf("Ostrava", "Praha", "Brno"),
            (repository.observeSavedPlaces().first() as Outcome.Success).data.map { it.name },
        )
        assertTrue("a reorder asks the host nothing", requests.isEmpty())
    }

    @Test
    fun `the suggestions are the bundled list less what is saved, in bundled order`() = runTest {
        val repository = repository(openMeteo())

        val all = (repository.observeSuggestedPlaces().first() as Outcome.Success).data
        assertEquals(SUGGESTED_COUNT, all.size)
        assertEquals(listOf("Praha", "Brno", "Ostrava"), all.take(3).map { it.name })

        repository.savePlace(all.first())

        val rest = (repository.observeSuggestedPlaces().first() as Outcome.Success).data
        assertEquals("a saved place is no longer a suggestion", SUGGESTED_COUNT - 1, rest.size)
        assertEquals("and the rest keep their order", listOf("Brno", "Ostrava"), rest.take(2).map { it.name })
        assertTrue(rest.none { it.id == all.first().id })
    }

    @Test
    fun `a forecast is fetched with the place's coordinates, once, and read from the table after`() = runTest {
        val repository = repository(openMeteo())
        repository.savePlace(PRAHA)

        val first = (repository.observeForecast(PRAHA.id).first() as Outcome.Success).data
        assertEquals(WeatherCode.Rain, first.current.code)
        assertEquals(1, requests.count { it.contains("forecast") })
        assertTrue(requests.last().contains("latitude=50.088") && requests.last().contains("longitude=14.421"))

        requests.clear()
        val cached = (repository.observeCachedForecast(PRAHA.id).first() as Outcome.Success).data
        assertEquals(first, cached)
        assertTrue("the cache asked nothing", requests.isEmpty())
    }

    @Test
    fun `a forecast for a place nothing knows is a failure, and the forecast host is not asked`() = runTest {
        val outcome = repository(openMeteo()).observeForecast(99).first()

        assertTrue((outcome as Outcome.Failure).error is NotFoundError)
        assertTrue(requests.none { "forecast.test" in it })
    }

    @Test
    fun `findPlace reads the saved row, then the bundled list, then asks the geocoder by id`() = runTest {
        val repository = repository(openMeteo())
        val saved = PRAHA.copy(name = "Prague, as saved")
        repository.savePlace(saved)

        assertEquals(Outcome.Success(saved), repository.findPlace(PRAHA.id))
        // Brno is bundled and not saved: known without a request.
        assertEquals(BRNO.id, (repository.findPlace(BRNO.id) as Outcome.Success).data.id)
        assertTrue(requests.isEmpty())

        assertEquals(Outcome.Success(HAMBURG), repository.findPlace(HAMBURG.id))
        assertEquals(listOf("https://geocoding.test/v1/get?id=${HAMBURG.id}"), requests)

        assertTrue((repository.findPlace(99) as Outcome.Failure).error is NotFoundError)
    }

    @Test
    fun `a forecast for a place that is not saved is fetched with the coordinates the lookup found`() = runTest {
        val outcome = repository(openMeteo()).observeForecast(HAMBURG.id).first()

        assertEquals(WeatherCode.Rain, (outcome as Outcome.Success).data.current.code)
        val forecastRequest = requests.single { "forecast.test" in it }
        assertTrue(forecastRequest.contains("latitude=53.55073") && forecastRequest.contains("longitude=9.99302"))
    }

    @Test
    fun `a failing host over a cached forecast emits the forecast and then the failure`() = runTest {
        repository(openMeteo()).let {
            it.savePlace(PRAHA)
            it.observeForecast(PRAHA.id).first()
        }

        val emissions = repository(failing()).observeForecast(PRAHA.id).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is Outcome.Success)
        assertTrue((emissions[1] as Outcome.Failure).error is ServerError)
    }

    @Test
    fun `a refresh writes the cache, and a failed one leaves it`() = runTest {
        val good = repository(openMeteo())
        good.savePlace(PRAHA)
        assertTrue(good.refreshForecast(PRAHA.id) is Outcome.Success)
        assertEquals(1, requests.count { it.contains("forecast") })

        assertTrue(repository(failing()).refreshForecast(PRAHA.id) is Outcome.Failure)
        assertTrue((good.observeCachedForecast(PRAHA.id).take(1).toList()[0] as Outcome.Success).data != null)
    }

    private companion object {

        val CONFIG =
            OpenMeteoConfig(
                geocodingBaseUrl = "https://geocoding.test/v1/",
                forecastBaseUrl = "https://forecast.test/v1/",
            )

        /** What `DefaultSuggestedPlacesDataSource` bundles. */
        const val SUGGESTED_COUNT = 30

        val PRAHA = Place(3067696, "Praha", "Česko", null, 50.088, 14.421, "Europe/Prague")
        val BRNO = Place(3078610, "Brno", "Česko", "Jihomoravský kraj", 49.195, 16.608, "Europe/Prague")
        val OSTRAVA = Place(3068799, "Ostrava", "Česko", "Moravskoslezský kraj", 49.835, 18.292, "Europe/Prague")

        /** Neither saved nor bundled: only the geocoder by id knows it. */
        val HAMBURG = Place(2911298, "Hamburg", "Deutschland", null, 53.55073, 9.99302, "Europe/Berlin")

        const val HAMBURG_JSON =
            """{"id":2911298,"name":"Hamburg","latitude":53.55073,"longitude":9.99302,"country_code":"DE",""" +
                """"timezone":"Europe/Berlin","country":"Deutschland","admin1":"Hamburg"}"""

        const val GEOCODING_JSON =
            """{"results":[{"id":3067696,"name":"Praha","latitude":50.08804,"longitude":14.42076,""" +
                """"country_code":"CZ","timezone":"Europe/Prague","country":"Česko","admin1":"Praha"}]}"""
        const val FORECAST_JSON = """{"latitude":50.08,"longitude":14.42,"timezone":"Europe/Prague",""" +
            """"current":{"time":"2026-09-20T15:45","interval":900,"temperature_2m":21.3,""" +
            """"weather_code":61,"wind_speed_10m":11.6},""" +
            """"hourly":{"time":["2026-09-20T00:00"],"temperature_2m":[12.1],""" +
            """"precipitation_probability":[0],"weather_code":[1]},""" +
            """"daily":{"time":["2026-09-20"],"weather_code":[61],"temperature_2m_max":[22.0],""" +
            """"temperature_2m_min":[11.5],""" +
            """"precipitation_sum":[1.2]}}"""
    }
}
