package com.example.androidproject1.feature.weather.data.repository

import com.example.androidproject1.feature.weather.data.source.FetchedForecast
import com.example.androidproject1.feature.weather.data.source.LocalWeatherDataSource
import com.example.androidproject1.feature.weather.data.source.RemoteWeatherDataSource
import com.example.androidproject1.feature.weather.data.source.SuggestedPlacesDataSource
import com.example.androidproject1.feature.weather.domain.Forecast
import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.feature.weather.domain.WeatherRepository
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.domain.result.map
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A forecast per place is a cache entry: `cached()` keyed by the place, so the detail draws what
 * it has and refreshes behind it, and the root reads the cache alone. A place is the user's —
 * saved and removed, never fetched twice — and the search is live.
 */
class DefaultWeatherRepository(
    logger: Logger,
    private val localWeatherDataSource: LocalWeatherDataSource,
    private val suggestedPlacesDataSource: SuggestedPlacesDataSource,
    private val remoteWeatherDataSource: RemoteWeatherDataSource,
) : WeatherRepository, BaseRepository(logger = logger.withTag("DefaultWeatherRepository")) {

    override suspend fun searchPlaces(query: String): Outcome<List<Place>> = execute {
        remoteWeatherDataSource.searchPlaces(query)
    }

    // Off the saved list rather than a list of its own: the offer is "what you have not kept
    // yet", so it has to change the moment something is saved or removed.
    override fun observeSuggestedPlaces(): Flow<Outcome<List<Place>>> = observe(
        source = localWeatherDataSource.observePlaces().map { saved ->
            val savedIds = saved.map { it.id }.toSet()
            suggestedPlacesDataSource.suggestedPlaces().filterNot { it.id in savedIds }
        },
        retries = RETRIES,
    )

    override fun observeSavedPlaces(): Flow<Outcome<List<Place>>> =
        observe(source = localWeatherDataSource.observePlaces(), retries = RETRIES)

    override suspend fun findPlace(placeId: Int): Outcome<Place> = execute { resolvePlace(placeId) }

    override suspend fun savePlace(place: Place): Outcome<Unit> = execute { localWeatherDataSource.savePlace(place) }

    override suspend fun movePlace(placeId: Int, toIndex: Int): Outcome<Unit> = execute {
        localWeatherDataSource.movePlace(placeId, toIndex)
    }

    override suspend fun removePlace(placeId: Int): Outcome<Unit> = execute {
        localWeatherDataSource.removePlace(placeId)
    }

    override fun observeForecast(placeId: Int): Flow<Outcome<Forecast>> = cached(
        local = localWeatherDataSource.observeForecast(placeId),
        remote = { fetch(placeId) },
        write = { localWeatherDataSource.storeForecast(placeId, it) },
    ).map { outcome -> outcome.map { it.forecast } }

    override fun observeCachedForecast(placeId: Int): Flow<Outcome<Forecast?>> =
        observe(source = localWeatherDataSource.observeForecast(placeId).map { it?.forecast }, retries = RETRIES)

    override suspend fun refreshForecast(placeId: Int): Outcome<Unit> = execute {
        localWeatherDataSource.storeForecast(placeId, fetch(placeId))
    }

    /** The host is asked with the place's coordinates, wherever [resolvePlace] found them. */
    private suspend fun fetch(placeId: Int): FetchedForecast {
        val place = resolvePlace(placeId)
        return remoteWeatherDataSource.fetchForecast(place.latitude, place.longitude)
    }

    /** Saved, then bundled — both without a request — then the geocoder by id, which throws `NotFoundError`. */
    private suspend fun resolvePlace(placeId: Int): Place =
        localWeatherDataSource.getPlace(placeId)
            ?: suggestedPlacesDataSource.suggestedPlaces().firstOrNull { it.id == placeId }
            ?: remoteWeatherDataSource.getPlace(placeId)

    private companion object {

        const val RETRIES = 3L
    }
}
