package com.example.androidproject1.feature.devmenu.presentation.devmenu

import androidx.navigation3.runtime.NavKey

/** One-off navigation intents, turned into back-stack calls in DevMenuDestination. */
sealed interface DevMenuNavigation {

    data object NavigateUp : DevMenuNavigation

    /** This feature's own, so the destination pushes it directly. */
    data object Playground : DevMenuNavigation

    data object NetworkLog : DevMenuNavigation

    /** A screen from the jump list — another feature's route, pushed onto whatever stack the menu is on. */
    data class Jump(val route: NavKey) : DevMenuNavigation
}
