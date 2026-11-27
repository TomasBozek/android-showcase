package com.example.androidproject1.feature.auth.presentation.verifyemail

/**
 * A right code starts the session and MainViewModel switches the flow from there, so success is
 * not navigation. Up is the only intent: back to sign-up, to fix the address.
 */
sealed interface VerifyEmailNavigation {

    data object NavigateUp : VerifyEmailNavigation
}
