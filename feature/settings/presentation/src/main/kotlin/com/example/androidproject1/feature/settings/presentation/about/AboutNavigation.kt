package com.example.androidproject1.feature.settings.presentation.about

/** One-off navigation intents, turned into back-stack calls in AboutDestination. */
sealed interface AboutNavigation {

    data object NavigateUp : AboutNavigation

    /**
     * The licences list. Its activity comes from Play services, which only `:app` depends on, so
     * the destination takes a lambda for it — the same door another feature's screen would use.
     */
    data object Licences : AboutNavigation
}
