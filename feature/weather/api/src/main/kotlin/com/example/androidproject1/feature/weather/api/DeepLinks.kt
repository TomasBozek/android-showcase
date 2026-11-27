package com.example.androidproject1.feature.weather.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.DeepLinkTarget

/**
 * `weather`, and `weather/<id>` — a place's forecast under it. `weather/search` is the
 * places screen, whose search is where a place is added — the launcher's Add a place.
 */
val weatherDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "weather" to DeepLinkTable { segments ->
        when (segments.size) {
            0 -> DeepLinkTarget(listOf(WeatherDestination))
            1 -> if (segments[0] == "search") {
                DeepLinkTarget(listOf(WeatherDestination))
            } else {
                segments[0].toIntOrNull()?.let {
                    DeepLinkTarget(listOf(WeatherDestination, ForecastDestination(placeId = it)))
                }
            }
            else -> null
        }
    },
)
