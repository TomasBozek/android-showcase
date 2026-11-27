package com.example.androidproject1.feature.movies.data.source

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.feature.movies.data.database.MovieEntity
import com.example.androidproject1.feature.movies.data.database.MoviesDatabase
import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.feature.movies.domain.MovieDetail
import com.example.androidproject1.feature.movies.domain.MoviePage
import com.example.androidproject1.service.core.domain.error.NetworkError
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Refresh, append and the end of pagination, against a fake remote and an in-memory database.
 *
 * The remote is a fake because what is being asked is the mediator's bookkeeping — which page it
 * fetches, what it writes, what it says about the end — and a fake can be told to fail on exactly
 * one of them. The database is real, because "the cache is left as it was" is only true of a table.
 */
@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class MoviesRemoteMediatorTest {

    private lateinit var database: MoviesDatabase
    private lateinit var local: DefaultLocalMoviesDataSource
    private var clock = 1_000_000L

    /** What was asked of the remote, in order. */
    private val asked = mutableListOf<Int>()
    private var failPage: Int? = null

    private val remote = object : RemoteMoviesDataSource {
        override suspend fun getPopular(page: Int): MoviePage {
            asked += page
            if (page == failPage) throw NetworkError()
            // TVmaze answers 404 past its last page, which the remote source reads as an empty last page.
            if (page > TOTAL_PAGES) return MoviePage(page, emptyList(), isLast = true)
            return MoviePage(page, listOf(movie(page * 10 + 1), movie(page * 10 + 2)), isLast = false)
        }

        override suspend fun getMovie(id: Int): MovieDetail = error("not asked")
    }

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
        local = DefaultLocalMoviesDataSource(database.moviesDao(), now = { clock })
    }

    @After
    fun tearDown() = database.close()

    private fun mediator(maxAge: Long = 60_000) =
        MoviesRemoteMediator(local, remote, now = { clock }, maxAgeMillis = maxAge)

    private fun state() = PagingState<Int, MovieEntity>(emptyList(), null, PagingConfig(pageSize = 2), 0)

    private suspend fun RemoteMediator<Int, MovieEntity>.run(type: LoadType) = load(type, state())

    private fun movie(id: Int) = Movie(id, "Movie $id", "", null, null, 7.0)

    private suspend fun cachedIds() = database.moviesDao().allMovies().map { it.id }

    @Test
    fun `refresh fetches page 1 and makes it the whole cache`() = runTest {
        val result = mediator().run(LoadType.REFRESH)

        assertEquals(listOf(1), asked)
        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf(11, 12), cachedIds())
    }

    @Test
    fun `append fetches the page after the last one cached, and writes only that page`() = runTest {
        val mediator = mediator()
        mediator.run(LoadType.REFRESH)

        val second = mediator.run(LoadType.APPEND)

        assertEquals(listOf(1, 2), asked)
        assertFalse((second as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf(11, 12, 21, 22), cachedIds())
    }

    @Test
    fun `the empty page past the last says the list is over, and an append after it asks nobody`() = runTest {
        val mediator = mediator()
        mediator.run(LoadType.REFRESH)
        mediator.run(LoadType.APPEND)
        val third = mediator.run(LoadType.APPEND)
        assertFalse(
            "TVmaze's last full page does not know it is last",
            (third as RemoteMediator.MediatorResult.Success).endOfPaginationReached,
        )

        val end = mediator.run(LoadType.APPEND)
        assertTrue((end as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf(1, 2, 3, 4), asked)

        val past = mediator.run(LoadType.APPEND)
        assertTrue((past as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals("no page 5 was asked for", listOf(1, 2, 3, 4), asked)
    }

    @Test
    fun `a refresh leaves exactly page 1, the pages after it gone until scrolled to`() = runTest {
        val mediator = mediator()
        mediator.run(LoadType.REFRESH)
        mediator.run(LoadType.APPEND)

        mediator.run(LoadType.REFRESH)

        assertEquals(listOf(11, 12), cachedIds())
        assertNull(local.readPage(2))
    }

    @Test
    fun `prepend never happens, the list only grows at its end`() = runTest {
        val result = mediator().run(LoadType.PREPEND)

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertTrue(asked.isEmpty())
    }

    @Test
    fun `a failed refresh is an error, and the cache is exactly as it was`() = runTest {
        val mediator = mediator()
        mediator.run(LoadType.REFRESH)
        mediator.run(LoadType.APPEND)
        failPage = 1

        val result = mediator.run(LoadType.REFRESH)

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(listOf(11, 12, 21, 22), cachedIds())
    }

    @Test
    fun `a failed append is an error, the page is not written, and asking again carries on from it`() = runTest {
        val mediator = mediator()
        mediator.run(LoadType.REFRESH)
        failPage = 2

        val result = mediator.run(LoadType.APPEND)
        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(listOf(11, 12), cachedIds())

        failPage = null
        mediator.run(LoadType.APPEND)
        assertEquals(listOf(11, 12, 21, 22), cachedIds())
    }

    @Test
    fun `an append onto an empty cache is not the end of the list`() = runTest {
        val result = mediator().run(LoadType.APPEND)

        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertTrue(asked.isEmpty())
    }

    @Test
    fun `a launch refreshes an empty cache and a stale one, and leaves a fresh one alone`() = runTest {
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator().initialize())

        mediator().run(LoadType.REFRESH)
        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, mediator().initialize())

        clock += 61_000
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator().initialize())
    }

    @Test
    fun `the cache knows how far it reaches`() = runTest {
        assertNull(local.cachedPages())
        val mediator = mediator()
        mediator.run(LoadType.REFRESH)
        mediator.run(LoadType.APPEND)

        val cached = local.cachedPages()
        assertNotNull(cached)
        assertEquals(2, cached!!.lastPage)
        assertFalse(cached.isComplete)
    }

    private companion object {

        const val TOTAL_PAGES = 3
    }
}
