package com.example.androidproject1.feature.movies.data.source

import androidx.paging.PagingSource
import com.example.androidproject1.feature.movies.data.database.MovieEntity
import com.example.androidproject1.feature.movies.domain.MovieDetail
import com.example.androidproject1.feature.movies.domain.MoviePage
import kotlinx.coroutines.flow.Flow

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:movies:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface LocalMoviesDataSource {

    /**
     * The listed movies as a source Paging reads pages of, invalidated when a page is written.
     * Entities, not movies: this is the data layer's own seam, and the repository maps.
     */
    fun pagingSource(): PagingSource<Int, MovieEntity>

    /** One page as it was cached: `null` until it has been fetched once, a page — empty or not — from then on. */
    suspend fun readPage(page: Int): MoviePage?

    /** When the list's first page was written, in epoch millis; `null` while nothing is cached. */
    fun observeLastUpdated(): Flow<Long?>

    /** How far the cache reaches, for the mediator to carry on from; `null` for an empty one. */
    suspend fun cachedPages(): CachedPages?

    /** Replaces that one page's rows and marker. */
    suspend fun replacePage(page: MoviePage)

    /** Replaces every page with this one — the refresh. */
    suspend fun replaceAll(page: MoviePage)

    /** The detail and the row it belongs to, or `null` when either is missing. */
    suspend fun getDetail(id: Int): MovieDetail?

    /** The detail, and the row too, so a deep link to a movie no page has listed still caches. */
    suspend fun storeDetail(detail: MovieDetail)
}

/**
 * What the cache holds: the last page it has ([lastPage]), whether that page was the list's end
 * ([isComplete]), and when the first page was fetched ([firstFetchedAt]) — the age of the list,
 * which decides whether a launch refreshes it.
 */
data class CachedPages(
    val lastPage: Int,
    val isComplete: Boolean,
    val firstFetchedAt: Long,
)
