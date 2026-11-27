package com.example.androidproject1.feature.movies.data.repository

import com.example.androidproject1.feature.movies.data.source.LocalMovieFavouritesDataSource
import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.feature.movies.domain.MovieFavouritesRepository
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.Flow

class DefaultMovieFavouritesRepository(
    logger: Logger,
    private val localMovieFavouritesDataSource: LocalMovieFavouritesDataSource,
) : MovieFavouritesRepository, BaseRepository(logger = logger.withTag("DefaultMovieFavouritesRepository")) {

    // retries, because these collectors outlive a failure: the favourites screen and the heart
    // hold their flows for as long as they are shown, so one transient database error must not
    // stop them emitting for good.
    override fun observeFavourites(): Flow<Outcome<List<Movie>>> =
        observe(source = localMovieFavouritesDataSource.observeFavourites(), retries = RETRIES)

    override fun observeIsFavourite(movieId: Int): Flow<Outcome<Boolean>> =
        observe(source = localMovieFavouritesDataSource.observeIsFavourite(movieId), retries = RETRIES)

    override suspend fun setFavourite(movieId: Int, favourite: Boolean): Outcome<Unit> =
        execute { localMovieFavouritesDataSource.setFavourite(movieId, favourite) }

    private companion object {

        const val RETRIES = 3L
    }
}
