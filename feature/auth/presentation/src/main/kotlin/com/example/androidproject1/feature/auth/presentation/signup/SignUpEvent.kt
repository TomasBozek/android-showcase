package com.example.androidproject1.feature.auth.presentation.signup

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface SignUpEvent : UiEvent {

    data class EmailChanged(val email: String) : SignUpEvent

    data class PasswordChanged(val password: String) : SignUpEvent

    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpEvent

    data object SignUpClicked : SignUpEvent

    /** The consent box. */
    data class TermsToggled(val accepted: Boolean) : SignUpEvent

    /** The "Terms of use" link inside the consent box's label. */
    data object TermsLinkClicked : SignUpEvent

    /** The "Privacy policy" link inside the consent box's label. */
    data object PrivacyLinkClicked : SignUpEvent

    data object LoginClicked : SignUpEvent

    /** The back gesture, on a form with something typed into it. */
    data object BackRequested : SignUpEvent
}
