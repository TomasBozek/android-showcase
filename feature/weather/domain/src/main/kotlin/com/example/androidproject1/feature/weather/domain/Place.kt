package com.example.androidproject1.feature.weather.domain

/** A place the geocoder knows: what the user searches for and saves. The wire shape is `PlaceDto`. */
data class Place(
    /** The geocoder's own id — stable, so a saved place is the same place tomorrow. */
    val id: Int,
    val name: String,
    val country: String,
    /** The region under the country — "Praha", "Bayern", "England" — or `null` for a city-state. */
    val region: String?,
    val latitude: Double,
    val longitude: Double,
    /** An IANA zone, so an hour in the forecast is the place's hour and not the phone's. */
    val timezone: String,
)
