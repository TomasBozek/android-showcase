package com.example.androidproject1.feature.movies.domain

import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

/**
 * Implemented in the data layer. Declared here so the domain layer depends on nothing.
 *
 * A favourite is the one thing a user writes into the movies feature: the id of a row and
 * when it was hearted, joined back onto the cached row when read, so what the list shows is the
 * movie and not a bare number.
 */
interface MovieFavouritesRepository {

    /** The favourited movies, newest first. */
    fun observeFavourites(): Flow<Outcome<List<Movie>>>

    fun observeIsFavourite(movieId: Int): Flow<Outcome<Boolean>>

    suspend fun setFavourite(movieId: Int, favourite: Boolean): Outcome<Unit>
}
