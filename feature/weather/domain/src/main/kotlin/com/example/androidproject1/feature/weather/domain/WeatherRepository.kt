package com.example.androidproject1.feature.weather.domain

import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

/**
 * Implemented in the data layer. Declared here so the domain layer depends on nothing.
 *
 * A place is what the user saves; a forecast is what the app fetches for it, cached per place so
 * the root screen reads it without a request and the detail refreshes it behind what it has.
 */
interface WeatherRepository {

    /** The geocoder, live: nothing is cached, a query is a round trip. */
    suspend fun searchPlaces(query: String): Outcome<List<Place>>

    /**
     * Places to pick from before anything has been typed: a bundled list less what is already
     * saved, in its own order. Nothing is fetched — it is the offer a first open makes,
     * not a search.
     */
    fun observeSuggestedPlaces(): Flow<Outcome<List<Place>>>

    /** The saved places, in the order the user arranged. */
    fun observeSavedPlaces(): Flow<Outcome<List<Place>>>

    /**
     * A place by the geocoder's id, saved or not: the saved row, else the bundled list, else the
     * geocoder asked by id — so a forecast link to a place nobody saved still has somewhere to be.
     * A `NotFoundError` when nothing knows the id.
     */
    suspend fun findPlace(placeId: Int): Outcome<Place>

    suspend fun savePlace(place: Place): Outcome<Unit>

    /**
     * Moves a saved place to [toIndex], the rest closing up behind it. By id rather than by a
     * `from` index: a drag knows the row it grabbed, and an index read off a list that has since
     * changed points at a different place.
     */
    suspend fun movePlace(placeId: Int, toIndex: Int): Outcome<Unit>

    suspend fun removePlace(placeId: Int): Outcome<Unit>

    /** The cached forecast first, if there is one; then what the host says; then the cache again. */
    fun observeForecast(placeId: Int): Flow<Outcome<Forecast>>

    /** The cached forecast alone — for a list that shows a number beside each place and asks nothing. */
    fun observeCachedForecast(placeId: Int): Flow<Outcome<Forecast?>>

    /** One round trip for this place, written to the cache; the failure is the caller's to show. */
    suspend fun refreshForecast(placeId: Int): Outcome<Unit>
}
