package com.example.androidproject1.feature.movies.data.source

import com.example.androidproject1.feature.movies.domain.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:movies:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface LocalMovieFavouritesDataSource {

    fun observeFavourites(): Flow<List<Movie>>

    fun observeIsFavourite(movieId: Int): Flow<Boolean>

    suspend fun setFavourite(movieId: Int, favourite: Boolean)
}
