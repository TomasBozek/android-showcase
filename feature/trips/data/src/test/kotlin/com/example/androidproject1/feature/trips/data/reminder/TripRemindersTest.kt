package com.example.androidproject1.feature.trips.data.reminder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.feature.trips.data.repository.DefaultTripsRepository
import com.example.androidproject1.feature.trips.data.source.LocalTripsDataSource
import com.example.androidproject1.feature.trips.domain.Trip
import com.example.androidproject1.feature.trips.domain.test.FakeTripsRepository
import com.example.androidproject1.feature.trips.domain.test.FakeTripsRepository.Companion.LISBON_TRIP
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.test.TestDispatchers
import com.example.androidproject1.service.device.test.FakeDeviceInfo
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.PostResult
import com.example.androidproject1.service.notification.test.FakeNotifier
import com.example.androidproject1.service.work.WorkResult
import com.example.androidproject1.service.work.test.FakeScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.hours

/**
 * The reminder end to end but for the clock: the switch schedules a job for 18:00 the evening
 * before (a fixed clock), off cancels it, and when the job runs it posts about the trip with its
 * deep link — through a real preferences store, a `FakeScheduler` and a `FakeNotifier`.
 */
@RunWith(RobolectricTestRunner::class)
class TripRemindersTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Two days before the Lisbon trip's first day, at noon: the evening is 30 hours away. */
    private val clock = Clock.fixed(Instant.parse("${LISBON_TRIP.startDate.minusDays(2)}T12:00:00Z"), ZoneOffset.UTC)

    private val trips = FakeTripsRepository()
    private val notifier = FakeNotifier()
    private val device = FakeDeviceInfo(applicationId = "com.example.app")
    private val scheduler = FakeScheduler()
    private var name = 0

    private fun work() = TripReminderWork(scheduler, DataStoreProvider(context, "reminders-${name++}"), TestDispatchers)

    private fun handler(work: TripReminderWork) = TripReminderHandler(trips, work, notifier, device)

    private fun reminders(work: TripReminderWork = work(), handler: TripReminderHandler = handler(work)) =
        DefaultTripReminders(trips, work, handler, clock)

    @Test
    fun `turning it on schedules a job for the evening before, with the delay to it`() = runTest {
        val reminders = reminders()

        reminders.set(LISBON_TRIP.id, on = true)

        val spec = scheduler.scheduled.single()
        assertEquals("trip-reminder-${LISBON_TRIP.id}", spec.name)
        assertEquals(TripReminderHandler.KEY, spec.handler)
        assertEquals(mapOf("tripId" to LISBON_TRIP.id), spec.input)
        assertEquals(30.hours, spec.delay)
        assertTrue(reminders.isOn(LISBON_TRIP.id).first())
    }

    @Test
    fun `turning it off cancels the job and clears the switch`() = runTest {
        val reminders = reminders()
        reminders.set(LISBON_TRIP.id, on = true)

        reminders.set(LISBON_TRIP.id, on = false)

        assertEquals(listOf("trip-reminder-${LISBON_TRIP.id}"), scheduler.cancelled)
        assertFalse(reminders.isOn(LISBON_TRIP.id).first())
    }

    @Test
    fun `an evening that has passed is on but schedules nothing`() = runTest {
        val late = Clock.fixed(Instant.parse("${LISBON_TRIP.startDate.minusDays(1)}T20:00:00Z"), ZoneOffset.UTC)
        val work = work()
        val reminders = DefaultTripReminders(trips, work, handler(work), late)

        reminders.set(LISBON_TRIP.id, on = true)

        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `a trip that is not there has no reminder to turn on`() = runTest {
        reminders().set("nowhere", on = true)

        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `when the job runs it posts a Reminders notification whose tap and action open the trip`() = runTest {
        val work = work()
        val reminders = reminders(work)
        reminders.set(LISBON_TRIP.id, on = true)

        val result = handler(work).run(mapOf("tripId" to LISBON_TRIP.id))

        assertEquals(WorkResult.Success, result)
        val posted = notifier.posted.single()
        assertEquals(NotificationKind.Reminders, posted.kind)
        assertEquals("com.example.app://trip/${LISBON_TRIP.id}", posted.deepLink)
        assertEquals(posted.deepLink, posted.actions.single().deepLink)
        assertEquals("trip-reminders", posted.group)
    }

    @Test
    fun `a switch turned off, or a trip deleted, since the job was scheduled posts nothing`() = runTest {
        val work = work()
        val handler = handler(work)

        assertEquals(WorkResult.Success, handler.run(mapOf("tripId" to LISBON_TRIP.id)))
        assertTrue(notifier.posted.isEmpty())

        reminders(work, handler).set(LISBON_TRIP.id, on = true)
        trips.deleteTrip(LISBON_TRIP.id)
        assertEquals(WorkResult.Success, handler.run(mapOf("tripId" to LISBON_TRIP.id)))
        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `a post the platform failed is tried again, one it refused is not`() = runTest {
        val work = work()
        val reminders = reminders(work)
        reminders.set(LISBON_TRIP.id, on = true)
        val handler = handler(work)

        notifier.result = PostResult.Failed
        assertEquals(WorkResult.Retry, handler.run(mapOf("tripId" to LISBON_TRIP.id)))

        notifier.result = PostResult.PermissionRefused
        assertEquals(WorkResult.Success, handler.run(mapOf("tripId" to LISBON_TRIP.id)))
    }

    @Test
    fun `the debug menu can post it now, whatever the switch says`() = runTest {
        val reminders = reminders()

        assertTrue(reminders.remindNow(LISBON_TRIP.id))
        assertEquals(1, notifier.posted.size)

        notifier.result = PostResult.PermissionRefused
        assertFalse(reminders.remindNow(LISBON_TRIP.id))
        assertFalse(reminders.remindNow("nowhere"))
    }

    @Test
    fun `deleting a trip through the repository takes its reminder with it`() = runTest {
        val work = work()
        reminders(work).set(LISBON_TRIP.id, on = true)
        val repository = DefaultTripsRepository(
            logger = FakeLogger(),
            localTripsDataSource = InMemoryTrips(listOf(LISBON_TRIP)),
            reminders = work,
        )

        repository.deleteTrip(LISBON_TRIP.id)

        assertEquals(listOf("trip-reminder-${LISBON_TRIP.id}"), scheduler.cancelled)
        assertFalse(work.isOn(LISBON_TRIP.id).first())
    }

    @Test
    fun `account deletion cancels every trip's job and forgets every switch`() = runTest {
        val work = work()
        val reminders = reminders(work)
        reminders.set(LISBON_TRIP.id, on = true)

        work.forgetAll(listOf(LISBON_TRIP.id, "other"))

        assertEquals(listOf("trip-reminder-${LISBON_TRIP.id}", "trip-reminder-other"), scheduler.cancelled)
        assertFalse(work.isOn(LISBON_TRIP.id).first())
    }

    private class InMemoryTrips(initial: List<Trip>) : LocalTripsDataSource {

        private val trips = MutableStateFlow(initial)

        override fun observeTrips() = trips

        override suspend fun getTrip(tripId: String) = trips.value.find { it.id == tripId }

        override suspend fun upsertTrip(trip: Trip) {
            trips.value = trips.value + trip
        }

        override suspend fun deleteTrip(tripId: String) {
            trips.value = trips.value.filterNot { it.id == tripId }
        }
    }
}
