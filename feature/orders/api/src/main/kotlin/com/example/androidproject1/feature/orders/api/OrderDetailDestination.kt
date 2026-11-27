package com.example.androidproject1.feature.orders.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** A route that carries arguments: the destination hands it whole to the screen's ViewModel. */
@Serializable
data class OrderDetailDestination(val orderId: String) : NavKey
