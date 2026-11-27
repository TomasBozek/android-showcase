package com.example.androidproject1.feature.trips.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class TripDetailDestination(val tripId: String) : NavKey
