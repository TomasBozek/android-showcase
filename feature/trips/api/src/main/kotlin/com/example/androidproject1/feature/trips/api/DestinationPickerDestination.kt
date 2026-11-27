package com.example.androidproject1.feature.trips.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @property resultKey where to put the chosen destination. Carried in the route rather than held
 * in a ViewModel, so it survives process death with the back stack entry it belongs to.
 */
@Serializable
data class DestinationPickerDestination(val resultKey: String) : NavKey
