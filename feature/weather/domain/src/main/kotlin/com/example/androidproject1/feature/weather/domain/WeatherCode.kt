package com.example.androidproject1.feature.weather.domain

/**
 * The WMO weather codes Open-Meteo sends (0–99), as the eight conditions a screen tells apart.
 *
 * An enum rather than the number: a screen draws a role, never a string it has to know the
 * meaning of, and a code the table does not name reads as [Cloudy] rather than crashing a list.
 */
enum class WeatherCode {
    Clear,
    Cloudy,
    Fog,
    Drizzle,
    Rain,
    Snow,
    Showers,
    Thunder,
    ;

    companion object {

        @Suppress("MagicNumber")
        fun from(wmo: Int): WeatherCode = when (wmo) {
            0, 1 -> Clear
            2, 3 -> Cloudy
            45, 48 -> Fog
            in 51..57 -> Drizzle
            in 61..67 -> Rain
            in 71..77, 85, 86 -> Snow
            in 80..82 -> Showers
            95, 96, 99 -> Thunder
            else -> Cloudy
        }
    }
}
