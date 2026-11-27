package com.example.androidproject1.feature.inventory.presentation.inventorydetail

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface InventoryDetailEvent : UiEvent {

    data class SectionSelected(val index: Int) : InventoryDetailEvent

    data object EditClicked : InventoryDetailEvent

    /** The picture: open it full screen. */
    data object ImageClicked : InventoryDetailEvent

    /** The full-screen viewer was closed. */
    data object ViewerClosed : InventoryDetailEvent

    data object DeleteClicked : InventoryDetailEvent

    /** A refused write: send it again. */
    data object RetrySyncClicked : InventoryDetailEvent

    /** A refused write: drop it, after asking. */
    data object DiscardSyncClicked : InventoryDetailEvent

    data object NavigateUpClicked : InventoryDetailEvent
}
