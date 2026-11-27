package com.example.androidproject1.feature.trips.data.reminder

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.androidproject1.feature.trips.domain.Trip
import com.example.androidproject1.feature.trips.domain.TripReminderTime
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.work.Scheduler
import com.example.androidproject1.service.work.WorkSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.time.toKotlinDuration

/**
 * A trip's reminder as the data layer keeps it: whether it is on, in the preferences store, and
 * the job that will fire it, in the scheduler. The two are changed together here and nowhere else,
 * so deleting a trip — from anywhere — leaves neither behind.
 */
class TripReminderWork(
    private val scheduler: Scheduler,
    dataStoreProvider: DataStoreProvider,
    private val dispatcherProvider: DispatcherProvider,
) {

    private val dataStore = dataStoreProvider.dataStore

    fun isOn(tripId: String): Flow<Boolean> = dataStore.data
        .map { it[key(tripId)] == true }
        .distinctUntilChanged()
        .flowOn(dispatcherProvider.io)

    /** Marks it on and schedules the job for [trip]; a trip whose evening has passed is marked and not scheduled. */
    suspend fun turnOn(trip: Trip, now: LocalDateTime) {
        edit { it[key(trip.id)] = true }
        val delay = TripReminderTime.delayFrom(now, trip.startDate)
        if (delay == null) {
            scheduler.cancel(name(trip.id))
        } else {
            scheduler.schedule(
                WorkSpec(
                    name = name(trip.id),
                    handler = TripReminderHandler.KEY,
                    input = mapOf(TripReminderHandler.TRIP_ID to trip.id),
                    delay = delay.toKotlinDuration(),
                ),
            )
        }
    }

    suspend fun turnOff(tripId: String) {
        scheduler.cancel(name(tripId))
        edit { it.remove(key(tripId)) }
    }

    /** A trip is gone: no reminder, on or scheduled. */
    suspend fun forget(tripId: String) = turnOff(tripId)

    /** Every reminder — what "delete my account" does. [tripIds] are the trips there were. */
    suspend fun forgetAll(tripIds: List<String>) {
        tripIds.forEach { scheduler.cancel(name(it)) }
        edit { prefs -> prefs.asMap().keys.filter { it.name.startsWith(PREFIX) }.forEach { prefs.remove(it) } }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        withContext(dispatcherProvider.io) { dataStore.edit(block) }
    }

    private fun key(tripId: String): Preferences.Key<Boolean> = booleanPreferencesKey(PREFIX + tripId)

    private fun name(tripId: String) = "trip-reminder-$tripId"

    private companion object {

        const val PREFIX = "trip_reminder_"
    }
}
