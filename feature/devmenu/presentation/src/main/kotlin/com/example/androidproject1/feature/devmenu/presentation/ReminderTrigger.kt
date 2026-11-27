package com.example.androidproject1.feature.devmenu.presentation

/**
 * Posts the reminder the app would post next, now, for the "Fire a reminder" button.
 *
 * A seam for the same reason [SessionReadout] is one: `:app` binds it in `wiring.trips`, and an app
 * with nothing to remind of binds nothing and the button is not drawn.
 */
interface ReminderTrigger {

    suspend fun fire(): Fired

    sealed interface Fired {

        /** Nothing is coming up, so there is nothing to remind of. */
        data object NothingToRemind : Fired

        /** Posted, for [title]. */
        data class Posted(val title: String) : Fired

        /** The user has notifications off. */
        data object Blocked : Fired
    }
}
