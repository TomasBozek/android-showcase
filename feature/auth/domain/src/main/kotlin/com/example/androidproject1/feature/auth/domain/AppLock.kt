package com.example.androidproject1.feature.auth.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The app lock: when switched on in Settings, the signed-in app is held behind a biometric
 * or the device credential at a cold start and again after five minutes away. It guards the
 * signed-in session, so it lives with the session's feature; who asks the question is the lock
 * screen's business, not this port's.
 */
interface AppLock {

    /** The Settings switch; off until turned on. */
    val enabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)

    /**
     * Whether the app is held right now: `true` only while [enabled] and not yet unlocked since a
     * cold start or a long enough absence. `null` until the setting has been read, so nothing is
     * drawn on a guess — a lock screen that flashes for a user who has it off is the bug this avoids.
     */
    val locked: StateFlow<Boolean?>

    /** The owner proved it: let them in until the next cold start or long absence. */
    fun unlocked()
}
