package com.example.androidproject1.feature.weather.data.source

import com.example.androidproject1.feature.weather.domain.Place
import kotlinx.coroutines.flow.Flow

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:weather:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface LocalWeatherDataSource {

    fun observePlaces(): Flow<List<Place>>

    suspend fun getPlace(placeId: Int): Place?

    suspend fun savePlace(place: Place)

    /** The order the user arranged: the place moves to [toIndex] and the rest close up. */
    suspend fun movePlace(placeId: Int, toIndex: Int)

    /** The place and its forecast. */
    suspend fun removePlace(placeId: Int)

    /** `null` until a forecast has been stored for the place; parsed and as stored. */
    fun observeForecast(placeId: Int): Flow<FetchedForecast?>

    suspend fun storeForecast(placeId: Int, forecast: FetchedForecast)
}
