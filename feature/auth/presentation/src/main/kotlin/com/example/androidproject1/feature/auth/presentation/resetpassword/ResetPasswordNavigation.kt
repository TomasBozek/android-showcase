package com.example.androidproject1.feature.auth.presentation.resetpassword

/** One-off navigation intents, turned into back-stack calls in ResetPasswordDestination. */
sealed interface ResetPasswordNavigation {

    /** The password is set: back to Login, which says so. */
    data object Done : ResetPasswordNavigation

    data object NavigateUp : ResetPasswordNavigation
}
