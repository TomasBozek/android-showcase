package com.example.androidproject1.feature.weather.domain

/**
 * Where Open-Meteo's two hosts are: a place search and the forecast, no key, because the API
 * asks for none. The same on every flavor, so the feature's own `di` binds [DEFAULT].
 */
data class OpenMeteoConfig(
    val geocodingBaseUrl: String,
    val forecastBaseUrl: String,
) {

    companion object {

        val DEFAULT = OpenMeteoConfig(
            geocodingBaseUrl = "https://geocoding-api.open-meteo.com/v1/",
            forecastBaseUrl = "https://api.open-meteo.com/v1/",
        )
    }
}
