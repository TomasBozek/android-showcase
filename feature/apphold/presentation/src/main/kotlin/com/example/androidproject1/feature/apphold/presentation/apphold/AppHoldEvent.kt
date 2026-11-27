package com.example.androidproject1.feature.apphold.presentation.apphold

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface AppHoldEvent : UiEvent {

    /** The one way past an update hold: the store. */
    data object UpdateClicked : AppHoldEvent
}
