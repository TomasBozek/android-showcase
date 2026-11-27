package com.example.androidproject1.feature.auth.presentation.verifyemail

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface VerifyEmailEvent : UiEvent {

    data class CodeChanged(val code: String) : VerifyEmailEvent

    data object SubmitClicked : VerifyEmailEvent

    /** Another code, once the countdown is over. */
    data object ResendClicked : VerifyEmailEvent

    data object NavigateUpClicked : VerifyEmailEvent
}
