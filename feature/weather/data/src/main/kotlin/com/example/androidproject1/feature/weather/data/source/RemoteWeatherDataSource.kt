package com.example.androidproject1.feature.weather.data.source

import com.example.androidproject1.feature.weather.domain.Forecast
import com.example.androidproject1.feature.weather.domain.Place

/**
 * A forecast with the JSON it was parsed from: the cache stores the wire shape whole, and the
 * repository hands the parsed one on, so one round trip serves both without a second parse.
 */
data class FetchedForecast(
    val json: String,
    val forecast: Forecast,
)

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:weather:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface RemoteWeatherDataSource {

    /** `v1/search?name=`. */
    suspend fun searchPlaces(query: String): List<Place>

    /** `v1/get?id=`: one place by the geocoder's id. A `NotFoundError` for an id it does not know. */
    suspend fun getPlace(placeId: Int): Place

    /** `v1/forecast?latitude=&longitude=…`, parsed and as sent — the cache stores the JSON whole. */
    suspend fun fetchForecast(latitude: Double, longitude: Double): FetchedForecast
}
