package com.example.androidproject1.feature.movies.data.source

import com.example.androidproject1.feature.movies.data.database.MoviePageEntity
import com.example.androidproject1.feature.movies.data.database.MoviesDao
import com.example.androidproject1.feature.movies.data.database.toDomain
import com.example.androidproject1.feature.movies.data.database.toEntities
import com.example.androidproject1.feature.movies.data.database.toEntity
import com.example.androidproject1.feature.movies.domain.MovieDetail
import com.example.androidproject1.feature.movies.domain.MoviePage

/**
 * The movies, read from and written to the database, one page at a time.
 *
 * A page reads as `null` until it has been fetched and as a [MoviePage] from then on; the rows
 * alone cannot tell the two apart, so the page marker written with them does — and carries the
 * whether it was the last, so a warm start knows where the list ends.
 */
class DefaultLocalMoviesDataSource(
    private val moviesDao: MoviesDao,
    private val now: () -> Long = System::currentTimeMillis,
) : LocalMoviesDataSource {

    override fun pagingSource() = moviesDao.pagingSource()

    override fun observeLastUpdated() = moviesDao.observeListFetchedAt()

    override suspend fun readPage(page: Int): MoviePage? {
        val marker = moviesDao.pageMarker(page) ?: return null
        val rows = moviesDao.page(page)
        return MoviePage(page = page, movies = rows.map { it.toDomain() }, isLast = marker.isLast)
    }

    override suspend fun cachedPages(): CachedPages? {
        val last = moviesDao.lastPageMarker() ?: return null
        // The list's age is its first page's: that is the one a refresh replaces everything from.
        val first = moviesDao.pageMarker(1) ?: last
        return CachedPages(lastPage = last.page, isComplete = last.isLast, firstFetchedAt = first.fetchedAt)
    }

    override suspend fun replacePage(page: MoviePage) =
        moviesDao.replacePage(page.page, page.toEntities(), page.marker())

    override suspend fun replaceAll(page: MoviePage) = moviesDao.replaceAll(page.toEntities(), page.marker())

    override suspend fun getDetail(id: Int): MovieDetail? {
        val detail = moviesDao.detail(id) ?: return null
        val movie = moviesDao.movie(id) ?: return null
        return detail.toDomain(movie.toDomain())
    }

    override suspend fun storeDetail(detail: MovieDetail) {
        // A row for a movie no page listed sits on page 0, position 0: never observed by a page,
        // there only so the detail can be joined back onto it.
        if (moviesDao.movie(detail.movie.id) == null) {
            moviesDao.insertMovie(detail.movie.toEntity(page = 0, position = 0))
        }
        moviesDao.insertDetail(detail.toEntity())
    }

    private fun MoviePage.marker() = MoviePageEntity(page = page, isLast = isLast, fetchedAt = now())
}
