package com.example.androidproject1.feature.weather.domain

import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.domain.result.map
import java.time.ZoneId

/**
 * The id of the one place that is "wherever I am now".
 *
 * A geocoder's ids are positive, so this cannot meet one. It is one row, saved like any other:
 * using the location again replaces it, which is what moving to another city should do.
 */
const val MY_LOCATION_ID = -1

/**
 * Saves where the device is as a place — [name] being the geocoder's locality, or what the caller
 * calls a place it could not name (`My location`) — and answers the [Place] saved, so it can be
 * opened.
 *
 * Latitude and longitude rather than a `Fix`: the domain is Kotlin/JVM and the location service is
 * an Android library. The country and region are empty because the platform's geocoder gave only a
 * name; the timezone is the device's, which is where a person asking for their own place is.
 */
suspend fun WeatherRepository.saveCurrentPlace(
    latitude: Double,
    longitude: Double,
    name: String,
    timezone: String = ZoneId.systemDefault().id,
): Outcome<Place> {
    val place = Place(
        id = MY_LOCATION_ID,
        name = name,
        country = "",
        region = null,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
    )
    return savePlace(place).map { place }
}
