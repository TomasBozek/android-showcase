package com.example.androidproject1.feature.auth.presentation.forgotpassword

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface ForgotPasswordEvent : UiEvent {

    data class EmailChanged(val email: String) : ForgotPasswordEvent

    data object SubmitClicked : ForgotPasswordEvent

    /** Another link to the same address, offered once the countdown is over. */
    data object ResendClicked : ForgotPasswordEvent

    data object NavigateUpClicked : ForgotPasswordEvent

    /** The back gesture, on a form with an address typed and not sent. */
    data object BackRequested : ForgotPasswordEvent
}
