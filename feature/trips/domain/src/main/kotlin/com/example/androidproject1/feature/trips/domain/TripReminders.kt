package com.example.androidproject1.feature.trips.domain

import kotlinx.coroutines.flow.Flow

/**
 * The reminder a trip can have: a notification at **18:00 the evening before its `startDate`**, so
 * the bag is packed while there is still an evening to pack it in.
 *
 * Implemented in the data layer, over the scheduler and the notifier. A reminder belongs to a trip:
 * deleting the trip takes it away, and so does [set] with `on = false`.
 */
interface TripReminders {

    /**
     * Turns the reminder on or off. On, for a trip whose evening has already passed, schedules
     * nothing: there is nothing left to remind of.
     */
    suspend fun set(tripId: String, on: Boolean)

    /** Whether [tripId] has a reminder set, following changes. `false` for a trip that is not there. */
    fun isOn(tripId: String): Flow<Boolean>

    /**
     * Posts the reminder for [tripId] **now**, whatever it is set to — what the debug menu fires to
     * see one. `false` when the trip is not there or the app may not post.
     */
    suspend fun remindNow(tripId: String): Boolean
}
