package com.example.androidproject1.feature.home.presentation.home

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface HomeEvent : UiEvent {

    /** A door's card was pressed. */
    data class DoorClicked(val id: String) : HomeEvent

    /** The speed dial's key, or its scrim: open or closed. */
    data class CreateToggled(val open: Boolean) : HomeEvent

    /** One of the dial's choices, by the contributed creation's id. */
    data class CreateChosen(val id: String) : HomeEvent

    /**
     * Whether this build has the developer screens, reported by the host through the same door a
     * user event comes in, so the screen still renders from its state and nothing else.
     */
    data class DeveloperDoorsAvailable(val available: Boolean) : HomeEvent
}
