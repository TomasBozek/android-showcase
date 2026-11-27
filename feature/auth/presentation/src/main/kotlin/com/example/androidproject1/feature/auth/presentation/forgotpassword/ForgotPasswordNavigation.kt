package com.example.androidproject1.feature.auth.presentation.forgotpassword

/** One-off navigation intents, turned into back-stack calls in ForgotPasswordDestination. */
sealed interface ForgotPasswordNavigation {

    /** Back to Login, which is where the link's new password will be used. */
    data object NavigateUp : ForgotPasswordNavigation
}
