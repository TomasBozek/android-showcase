package com.example.androidproject1.feature.auth.presentation.applock

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface AppLockEvent : UiEvent {

    /** The Unlock button: ask for the proof again, after the first prompt was backed out of. */
    data object UnlockClicked : AppLockEvent
}
