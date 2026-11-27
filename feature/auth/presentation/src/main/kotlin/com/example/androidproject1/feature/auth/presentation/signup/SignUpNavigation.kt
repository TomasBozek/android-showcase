package com.example.androidproject1.feature.auth.presentation.signup

/**
 * Signing up no longer changes the session: the account is created and a code mailed, and
 * the session starts on [VerifyEmail]. [Login] is back to the sign-in screen.
 */
sealed interface SignUpNavigation {

    data object Login : SignUpNavigation

    /** The code sign-up mailed to [email]. */
    data class VerifyEmail(val email: String) : SignUpNavigation
}
