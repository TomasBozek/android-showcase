package com.example.androidproject1.service.core.domain

/**
 * One feature's share of what "delete my account" removes from the device, and — unless
 * [erasesOnSignOut] says otherwise — of what a sign-out removes too.
 *
 * Account data is spread across every feature's own store — a Room database here, a handful of
 * DataStore keys there, a directory of pictures — and no single place can name them all without
 * depending on every feature. So each feature binds one of these in its `di` module, and the auth
 * feature runs them all through `getAll()` between the server's confirmation and the session's
 * end. Device facts — the theme, the language, the onboarding flag — are not account data and
 * are not erased.
 *
 * [erase] is allowed to fail, and a failure is meant to stop the deletion: the session is cleared
 * only after every eraser has returned, so a half-wiped device is never signed out over its own
 * leftovers.
 */
public interface LocalDataEraser {

    /**
     * Whether a sign-out runs this too. Account data does, so the next account on the phone starts
     * from nothing; what belongs to the install rather than to whoever was signed in — the
     * telemetry install id — goes only with the account.
     */
    public val erasesOnSignOut: Boolean get() = true

    public suspend fun erase()
}
