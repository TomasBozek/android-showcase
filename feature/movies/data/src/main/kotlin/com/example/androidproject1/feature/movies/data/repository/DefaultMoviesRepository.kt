package com.example.androidproject1.feature.movies.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.androidproject1.feature.movies.data.database.toDomain
import com.example.androidproject1.feature.movies.data.source.LocalMoviesDataSource
import com.example.androidproject1.feature.movies.data.source.MoviesRemoteMediator
import com.example.androidproject1.feature.movies.data.source.RemoteMoviesDataSource
import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.feature.movies.domain.MovieDetail
import com.example.androidproject1.feature.movies.domain.MoviesRepository
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * The list is a `Pager` over the database with a remote mediator filling it: what is on
 * screen is always the cache, and TVmaze is asked when the list reaches its end, when it is pulled,
 * or when a launch finds it stale. A detail is one row, local or fetched.
 */
@OptIn(ExperimentalPagingApi::class)
class DefaultMoviesRepository(
    logger: Logger,
    private val localMoviesDataSource: LocalMoviesDataSource,
    private val remoteMoviesDataSource: RemoteMoviesDataSource,
) : MoviesRepository, BaseRepository(logger = logger.withTag("DefaultMoviesRepository")) {

    override fun pagedMovies(): Flow<PagingData<Movie>> = Pager(
        config = PagingConfig(
            // What the screen reads at a time; a TVmaze page the mediator writes holds 250.
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = PREFETCH,
            // A placeholder needs a total the server gives per page, not for the list.
            enablePlaceholders = false,
        ),
        remoteMediator = MoviesRemoteMediator(localMoviesDataSource, remoteMoviesDataSource),
        pagingSourceFactory = localMoviesDataSource::pagingSource,
    ).flow.map { data -> data.map { it.toDomain() } }

    override fun observeLastUpdated(): Flow<Instant?> =
        localMoviesDataSource.observeLastUpdated().map { millis -> millis?.let(Instant::ofEpochMilli) }

    // Not cached() either: the happy path is a row already listed, so a miss is one network call
    // that is then stored — a deep link to a movie no page has shown still opens.
    override suspend fun getMovie(id: Int): Outcome<MovieDetail> = execute {
        localMoviesDataSource.getDetail(id)
            ?: remoteMoviesDataSource.getMovie(id).also { localMoviesDataSource.storeDetail(it) }
    }

    private companion object {

        const val PAGE_SIZE = 20
        const val PREFETCH = 5
    }
}
