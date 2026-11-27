package com.example.androidproject1.feature.movies.data.repository

import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.feature.movies.data.database.MoviesDatabase
import com.example.androidproject1.feature.movies.data.source.DefaultLocalMoviesDataSource
import com.example.androidproject1.feature.movies.data.source.DefaultRemoteMoviesDataSource
import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.feature.movies.domain.MoviePage
import com.example.androidproject1.feature.movies.domain.TvMazeConfig
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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * The paged list, end to end: a real `MockEngine` on one side answering TVmaze's shape, a real
 * SQLite on the other, and `asSnapshot` reading what the `Pager` between them shows.
 *
 * Faking either would leave the interesting part — that what the network sends is what the cache
 * reads back and what the list shows, and that a failing network leaves the rows alone — asserted
 * by nothing. The mediator's own cases are `MoviesRemoteMediatorTest`.
 */
@RunWith(RobolectricTestRunner::class)
class DefaultMoviesRepositoryTest {

    private lateinit var database: MoviesDatabase

    /** Every request the engine saw, so a test can say "and the network was not asked". */
    private val requests = mutableListOf<String>()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoviesDatabase::class.java,
        )
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    private fun repository(engine: MockEngine, now: () -> Long = System::currentTimeMillis) = DefaultMoviesRepository(
        logger = FakeLogger(),
        localMoviesDataSource = DefaultLocalMoviesDataSource(database.moviesDao(), now),
        remoteMoviesDataSource = DefaultRemoteMoviesDataSource(
            client = HttpClientFactory.create(
                engine = engine,
                // The template's own host: a TVmaze request must leave it alone.
                config = NetworkConfig(baseUrl = "https://fixtures.test/"),
            ),
            dispatcherProvider = TestDispatchers,
            config = CONFIG,
        ),
    )

    /**
     * Answers `shows?page=n` — numbered from 0, [pageSize] rows each — and `shows/<id>` from the same
     * rows, the way TVmaze does: a page past the last is a `404`, and there is no key.
     */
    private fun tvmaze(pages: Int = 3, pageSize: Int = 2) = MockEngine { request ->
        requests += request.url.toString()
        val url = request.url
        assertEquals("api.tvmaze.test", url.host)
        assertNull("TVmaze takes no key", url.parameters["api_key"])
        val route = url.segments
        when {
            route == listOf("shows") -> {
                val index = url.parameters["page"]!!.toInt()
                if (index >= pages) {
                    respondError(HttpStatusCode.NotFound)
                } else {
                    respondJson((1..pageSize).joinToString(",", "[", "]") { row(id = index * pageSize + it) })
                }
            }

            route.size == 2 && route[0] == "shows" -> {
                val id = route[1].toInt()
                if (id > pages * pageSize) respondError(HttpStatusCode.NotFound) else respondJson(row(id))
            }

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

    /** What `pagedMovies()` shows once it has loaded what it was asked for, as titles. */
    private suspend fun DefaultMoviesRepository.titles(scrollTo: Int? = null): List<String> =
        pagedMovies().asSnapshot { scrollTo?.let { scrollTo(it) } }.map { it.title }

    @Test
    fun `the list's write time is stored with the cache, and none before a fetch`() = runTest {
        val repository = repository(tvmaze(), now = { WRITTEN_AT })
        assertNull(repository.observeLastUpdated().first())

        repository.titles()

        assertEquals(Instant.ofEpochMilli(WRITTEN_AT), repository.observeLastUpdated().first())
    }

    @Test
    fun `the first read is the network's, through the cache`() = runTest {
        val titles = repository(tvmaze()).titles()

        assertEquals(listOf("Movie 1", "Movie 2"), titles.take(2))
        assertTrue("TVmaze's page 0 was asked for first", requests.first().endsWith("shows?page=0"))
        assertEquals("what was shown is what the table holds", titles.size, database.moviesDao().allMovies().size)
    }

    @Test
    fun `the list is readable to its end, page after page, and no page past it is asked for`() = runTest {
        val titles = repository(tvmaze(pages = 3)).titles(scrollTo = 5)

        assertEquals((1..6).map { "Movie $it" }, titles)
        assertTrue("the second page was asked for", requests.any { it.endsWith("page=1") })
        assertTrue("the 404 past the last page ended the list", requests.any { it.endsWith("page=3") })
        assertTrue("nothing past the 404 was asked for", requests.none { it.endsWith("page=4") })
    }

    @Test
    fun `a fresh, complete cache is read without asking the network`() = runTest {
        val local = DefaultLocalMoviesDataSource(database.moviesDao())
        local.replacePage(MoviePage(1, listOf(movie(1), movie(2)), isLast = true))

        val titles = repository(tvmaze()).titles()

        assertEquals(listOf("Movie 1", "Movie 2"), titles)
        assertTrue("a warm start reads the table and asks nobody", requests.isEmpty())
    }

    @Test
    fun `with the remote failing, the failure reaches the collector and the cached rows stay where they are`() =
        runTest {
            // Cached long ago, so a launch refreshes it: and the refresh fails.
            val longAgo = DefaultLocalMoviesDataSource(database.moviesDao(), now = { 0L })
            longAgo.replacePage(MoviePage(1, listOf(movie(1), movie(2)), isLast = false))

            val failure = runCatching { repository(failing()).titles() }.exceptionOrNull()

            assertTrue("the refresh was tried and its error is the collector's to show", failure is ServerError)
            assertTrue(requests.isNotEmpty())
            // The offline read: what the list would show is the table, and the table is untouched.
            val shown = DefaultLocalMoviesDataSource(database.moviesDao()).pagingSource()
                .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
            assertEquals(
                listOf("Movie 1", "Movie 2"),
                (shown as PagingSource.LoadResult.Page).data.map { it.title },
            )
        }

    private fun movie(id: Int) = Movie(
        id = id,
        title = "Movie $id",
        overview = "About $id.",
        posterUrl = null,
        releaseDate = null,
        rating = 7.0,
    )

    @Test
    fun `a movie is fetched once on a cold cache and never on a warm one`() = runTest {
        val repository = repository(tvmaze())

        val first = repository.getMovie(3)
        requests.clear()
        val second = repository.getMovie(3)

        val detail = (first as Outcome.Success).data
        assertEquals("Movie 3", detail.movie.title)
        assertEquals("the summary's HTML is gone", "About 3 & more.", detail.movie.overview)
        assertEquals(60.minutes, detail.runtime)
        assertEquals(listOf("Drama", "Thriller"), detail.genres)
        assertNull("TVmaze has no tagline", detail.tagline)
        assertEquals("https://static.test/original/3.jpg", detail.backdropUrl)
        assertEquals("https://static.test/medium/3.jpg", detail.movie.posterUrl)
        assertEquals(detail, (second as Outcome.Success).data)
        assertTrue("the second read was the table's", requests.isEmpty())
    }

    @Test
    fun `a movie the server does not have is a failure, not a row`() = runTest {
        val outcome = repository(tvmaze()).getMovie(99)

        assertTrue(outcome is Outcome.Failure)
        assertNull(database.moviesDao().movie(99))
    }

    /** A show in TVmaze's shape; the list and the detail answer the same object. */
    private fun row(id: Int) =
        """{"id":$id,"name":"Movie $id","genres":["Drama","Thriller"],"runtime":60,""" +
            """"premiered":"2001-01-0${id % 10}",""" +
            """"rating":{"average":7.5},"image":{"medium":"https://static.test/medium/$id.jpg",""" +
            """"original":"https://static.test/original/$id.jpg"},"summary":"<p><b>About $id</b> &amp; more.</p>"}"""

    private companion object {

        val CONFIG = TvMazeConfig(apiBaseUrl = "https://api.tvmaze.test/")
    }
}

private const val WRITTEN_AT = 1_790_000_000_000L
