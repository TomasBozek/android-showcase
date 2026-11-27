package com.example.androidproject1.feature.trips.data.repository

import com.example.androidproject1.feature.trips.data.database.TripsDatabase
import com.example.androidproject1.feature.trips.data.reminder.TripReminderWork
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.withContext

/**
 * The trips feature's share of "delete my account": every table of [TripsDatabase].
 *
 * `clearAllTables` is blocking and Room refuses it on the main thread, so it runs on IO here —
 * the same rule the data sources follow, since `BaseRepository` runs on the caller's context.
 */
class DefaultTripsEraser(
    private val database: TripsDatabase,
    private val dispatcherProvider: DispatcherProvider,
    private val reminders: TripReminderWork,
) : LocalDataEraser {

    override suspend fun erase() {
        // The reminders first: with the trips gone there is nothing to say which jobs to cancel.
        val ids = withContext(dispatcherProvider.io) { database.tripDao().tripIds() }
        reminders.forgetAll(ids)
        withContext(dispatcherProvider.io) { database.clearAllTables() }
    }
}
