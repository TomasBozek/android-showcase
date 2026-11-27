package com.example.androidproject1.feature.trips.data.reminder

import com.example.androidproject1.feature.trips.domain.TripReminders
import com.example.androidproject1.feature.trips.domain.TripsRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDateTime

/** [TripReminders] over [TripReminderWork]: on schedules the job for the trip's evening, off cancels it. */
class DefaultTripReminders(
    private val tripsRepository: TripsRepository,
    private val work: TripReminderWork,
    private val handler: TripReminderHandler,
    private val clock: Clock,
) : TripReminders {

    override suspend fun set(tripId: String, on: Boolean) {
        if (!on) {
            work.turnOff(tripId)
            return
        }
        val trip = (tripsRepository.getTrip(tripId) as? Outcome.Success)?.data ?: return
        work.turnOn(trip, LocalDateTime.now(clock))
    }

    override fun isOn(tripId: String): Flow<Boolean> = work.isOn(tripId)

    override suspend fun remindNow(tripId: String): Boolean = handler.post(tripId)
}
