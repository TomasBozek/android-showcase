package com.example.androidproject1.feature.inventory.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** A route that carries the item's id; the screen observes it, so an edit shows up without a reload. */
@Serializable
data class InventoryDetailDestination(val itemId: String) : NavKey
