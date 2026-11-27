package com.example.androidproject1.feature.settings.presentation.about

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface AboutEvent : UiEvent {

    /** The open-source licences: a screen the Play services library draws, reached through `:app`. */
    data object LicencesClicked : AboutEvent

    data object PrivacyClicked : AboutEvent

    data object TermsClicked : AboutEvent

    /** The Up arrow was tapped. Every non-root screen has one. */
    data object NavigateUpClicked : AboutEvent
}
