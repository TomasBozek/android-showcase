package com.example.androidproject1.feature.weather.data.source

import com.example.androidproject1.feature.weather.domain.OpenMeteoConfig
import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.network.HttpErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

/**
 * Open-Meteo over the shared client, on absolute URLs — two hosts, no key.
 *
 * Switching to IO is this class's job, not the repository's: `BaseRepository` runs on the caller's
 * context, and the caller is `viewModelScope`. Failures are mapped here so the repository above
 * sees a `DomainError` and never an HTTP status.
 */
class DefaultRemoteWeatherDataSource(
    private val client: HttpClient,
    private val dispatcherProvider: DispatcherProvider,
    private val config: OpenMeteoConfig,
) : RemoteWeatherDataSource {

    override suspend fun searchPlaces(query: String): List<Place> = request {
        client.get(config.geocodingBaseUrl + "search") {
            parameter("name", query)
            parameter("count", SEARCH_COUNT)
        }.body<GeocodingResponseDto>().results.map { it.toDomain() }
    }

    override suspend fun getPlace(placeId: Int): Place = request {
        client.get(config.geocodingBaseUrl + "get") {
            parameter("id", placeId)
        }.body<PlaceDto>().toDomain()
    }

    override suspend fun fetchForecast(latitude: Double, longitude: Double): FetchedForecast = request {
        val json = client.get(config.forecastBaseUrl + "forecast") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", "temperature_2m,weather_code,wind_speed_10m")
            parameter("hourly", "temperature_2m,precipitation_probability,weather_code")
            parameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum")
            parameter("timezone", "auto")
            parameter("forecast_days", FORECAST_DAYS)
        }.bodyAsText()
        // Straight off the wire, so it was fetched now; the cache overwrites this with the
        // row's own timestamp when it reads it back.
        FetchedForecast(
            json = json,
            forecast = parser.decodeFromString<ForecastDto>(json).toDomain(LocalDateTime.now()),
        )
    }

    private val parser = Json { ignoreUnknownKeys = true }

    private suspend fun <T> request(block: suspend () -> T): T = withContext(dispatcherProvider.io) {
        try {
            block()
        } catch (throwable: Throwable) {
            throw HttpErrorMapper.map(throwable)
        }
    }

    private companion object {

        const val SEARCH_COUNT = 10
        const val FORECAST_DAYS = 7
    }
}
