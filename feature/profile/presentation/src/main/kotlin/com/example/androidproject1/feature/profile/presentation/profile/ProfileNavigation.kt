package com.example.androidproject1.feature.profile.presentation.profile

import androidx.navigation3.runtime.NavKey

/** One-off navigation intents, turned into back-stack calls in ProfileDestination. */
sealed interface ProfileNavigation {

    data object NavigateUp : ProfileNavigation

    /** A contributed door's route — another feature's screen, pushed. */
    data class Open(val route: NavKey) : ProfileNavigation
}
