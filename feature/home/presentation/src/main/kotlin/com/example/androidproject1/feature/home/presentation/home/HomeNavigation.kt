package com.example.androidproject1.feature.home.presentation.home

import androidx.navigation3.runtime.NavKey

/**
 * Home is the crossroads: a door to every place. What a door opens is another feature's
 * screen or tab, so the intent carries the door and `AppNavHost` — the one file that knows every
 * destination — decides whether that is a tab to select or a screen to push.
 */
sealed interface HomeNavigation {

    /** A door's route: a tab root is selected, anything else pushed — the host's call. */
    data class Open(val route: NavKey) : HomeNavigation

    /** The speed dial's choice: the route its creation gave for this press, pushed. */
    data class Create(val route: NavKey) : HomeNavigation
}
