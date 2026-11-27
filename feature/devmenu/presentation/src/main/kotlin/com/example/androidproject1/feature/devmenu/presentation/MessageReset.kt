package com.example.androidproject1.feature.devmenu.presentation

/**
 * Forgets which server messages were dismissed, so they show again on the next start.
 *
 * A seam for the same reason `NotificationTester` is one: the messages and the store they are
 * dismissed in are `:app`'s, which this feature cannot name.
 */
interface MessageReset {

    suspend fun reset()
}
