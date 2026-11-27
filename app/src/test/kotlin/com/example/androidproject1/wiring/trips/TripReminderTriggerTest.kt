package com.example.androidproject1.wiring.trips

import com.example.androidproject1.feature.devmenu.presentation.ReminderTrigger
import com.example.androidproject1.feature.trips.domain.test.FakeTripReminders
import com.example.androidproject1.feature.trips.domain.test.FakeTripsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TripReminderTriggerTest {

    private val reminders = FakeTripReminders()

    // A fixed day: the fixture's trip has dates, and "upcoming" must not depend on today's.
    private val dayBefore = { FakeTripsRepository.LISBON_TRIP.startDate.minusDays(1) }

    @Test
    fun `fires for the soonest upcoming trip`() = runTest {
        val fired = TripReminderTrigger(FakeTripsRepository(), reminders, today = dayBefore).fire()

        assertEquals(listOf(FakeTripsRepository.LISBON_TRIP.id), reminders.reminded)
        assertEquals(ReminderTrigger.Fired.Posted(FakeTripsRepository.LISBON_TRIP.name), fired)
    }

    @Test
    fun `with no upcoming trip there is nothing to fire`() = runTest {
        val fired = TripReminderTrigger(FakeTripsRepository(initial = emptyList()), reminders, today = dayBefore).fire()

        assertEquals(emptyList<String>(), reminders.reminded)
        assertEquals(ReminderTrigger.Fired.NothingToRemind, fired)
    }
}
