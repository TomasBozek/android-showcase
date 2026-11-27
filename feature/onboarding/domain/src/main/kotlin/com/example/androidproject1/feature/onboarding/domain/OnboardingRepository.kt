package com.example.androidproject1.feature.onboarding.domain

import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

/**
 * Implemented in the data layer. Declared here so the domain layer depends on nothing.
 *
 * One flag, observed rather than read once, because it is what decides the flow the app is in:
 * `MainViewModel` combines it with the session, and the onboarding screen finishes by writing it
 * and letting that combination move the app on. A read would leave the tour on screen.
 */
interface OnboardingRepository {

    /** `false` until the tour has been finished once — a first run is not a failure. */
    fun observeSeen(): Flow<Outcome<Boolean>>

    suspend fun markSeen(): Outcome<Unit>

    /**
     * `false` until the tour has been walked or skipped once.
     *
     * Its own flag beside [observeSeen]: onboarding is what the app says before you are in, and
     * the tour what it says once you are, so finishing one says nothing about the other — and
     * replaying the tour must not put the pager back.
     */
    fun observeTourSeen(): Flow<Outcome<Boolean>>

    suspend fun markTourSeen(): Outcome<Unit>

    /** Puts the flag back, so the tour runs again the next time Home is on screen. */
    suspend fun resetTour(): Outcome<Unit>

    /**
     * The `versionCode` whose What's new was last shown; `0` on a first run, and on an
     * install from before this was stored.
     */
    suspend fun lastSeenVersionCode(): Outcome<Int>

    suspend fun setLastSeenVersionCode(versionCode: Int): Outcome<Unit>
}
