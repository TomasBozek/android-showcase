package com.example.androidproject1.feature.auth.presentation.resetpassword

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface ResetPasswordEvent : UiEvent {

    data class PasswordChanged(val password: String) : ResetPasswordEvent

    data class ConfirmPasswordChanged(val confirmPassword: String) : ResetPasswordEvent

    data object SubmitClicked : ResetPasswordEvent

    data object NavigateUpClicked : ResetPasswordEvent

    /** The back gesture, with something typed. */
    data object BackRequested : ResetPasswordEvent
}
