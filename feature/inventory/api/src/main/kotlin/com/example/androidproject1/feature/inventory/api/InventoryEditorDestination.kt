package com.example.androidproject1.feature.inventory.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * A route that carries the item's id. A new item is a fresh id the list minted, so create and
 * edit are one destination and one ViewModel.
 */
@Serializable
data class InventoryEditorDestination(val itemId: String) : NavKey
