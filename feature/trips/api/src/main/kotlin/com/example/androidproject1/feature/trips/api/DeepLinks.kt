package com.example.androidproject1.feature.trips.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import com.example.androidproject1.service.deeplink.withId

/**
 * `trips`, the tab; `trips/new`, the wizard over it — the launcher's New trip; and
 * `trip/<id>`, a trip under it.
 */
val tripsDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "trips" to DeepLinkTable { segments ->
        when (segments) {
            emptyList<String>() -> DeepLinkTarget(listOf(TripsDestination))
            listOf("new") -> DeepLinkTarget(listOf(TripsDestination, TripWizardDestination))
            else -> null
        }
    },
    "trip" to withId({ it }) { DeepLinkTarget(listOf(TripsDestination, TripDetailDestination(tripId = it))) },
)
