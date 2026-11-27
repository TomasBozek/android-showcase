package com.example.androidproject1.feature.movies.data.source

import com.example.androidproject1.feature.movies.data.database.MovieFavouriteEntity
import com.example.androidproject1.feature.movies.data.database.MovieFavouritesDao
import com.example.androidproject1.feature.movies.data.database.toDomain
import com.example.androidproject1.feature.movies.domain.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The favourites, read from and written to the movies database. A read is the join in the
 * DAO; a write is one row keyed by the movie's id, stamped with the clock so the list has an
 * order.
 */
class DefaultLocalMovieFavouritesDataSource(
    private val movieFavouritesDao: MovieFavouritesDao,
    private val now: () -> Long = System::currentTimeMillis,
) : LocalMovieFavouritesDataSource {

    override fun observeFavourites(): Flow<List<Movie>> =
        movieFavouritesDao.observeFavourites().map { rows -> rows.map { it.toDomain() } }

    override fun observeIsFavourite(movieId: Int): Flow<Boolean> = movieFavouritesDao.observeIsFavourite(movieId)

    override suspend fun setFavourite(movieId: Int, favourite: Boolean) {
        if (favourite) {
            movieFavouritesDao.add(MovieFavouriteEntity(id = movieId, favouritedAt = now()))
        } else {
            movieFavouritesDao.remove(movieId)
        }
    }
}
