package com.example.androidproject1.feature.devmenu.presentation

import kotlinx.coroutines.flow.Flow

/**
 * The signed-in address, for the session row.
 *
 * A seam rather than the auth feature's own service, so the menu names no feature: `:app` binds it
 * in `wiring.auth`, and an app without sign-in binds nothing and the row is not drawn.
 */
interface SessionReadout {

    /** The signed-in address, or `null` while signed out. Never the id. */
    fun signedIn(): Flow<String?>
}
