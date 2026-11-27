package com.example.androidproject1.feature.movies.domain

import androidx.paging.PagingData
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Implemented in the data layer. Declared here so the domain layer depends on nothing.
 *
 * The list is Paging 3: the cache is what is read and the network fills it as the list
 * reaches its end.
 */
interface MoviesRepository {

    /**
     * The list of shows, paged. What arrives is always the database's; TVmaze is asked at the end of
     * the list, on a refresh, and at launch when the cache is old. A failure leaves the rows where
     * they are and reaches the collector as a load state — `refresh()` and `retry()` on the
     * `LazyPagingItems` are the way to ask again.
     */
    fun pagedMovies(): Flow<PagingData<Movie>>

    /**
     * When the list on screen was fetched — its first page's write time, the one a refresh replaces
     * — or `null` while nothing is cached. Emits again after every refresh.
     */
    fun observeLastUpdated(): Flow<Instant?>

    /** The cached detail, or one network round trip that is then cached. */
    suspend fun getMovie(id: Int): Outcome<MovieDetail>
}
