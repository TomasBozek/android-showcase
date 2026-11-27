package com.example.androidproject1.feature.devmenu.presentation.networklog

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface NetworkLogEvent : UiEvent {

    data class EntryClicked(val id: Long) : NetworkLogEvent

    data object DetailDismissed : NetworkLogEvent

    /** The open exchange, as text, to the clipboard. */
    data object CopyClicked : NetworkLogEvent

    data object ClearClicked : NetworkLogEvent

    data object UpClicked : NetworkLogEvent
}
