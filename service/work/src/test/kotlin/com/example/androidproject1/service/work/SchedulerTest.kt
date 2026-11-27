package com.example.androidproject1.service.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * The scheduler against the real WorkManager, on its test driver: a delay, a constraint and a
 * period are each held until the driver says they are met, which is what a device's clock and
 * network would do.
 */
@RunWith(RobolectricTestRunner::class)
class SchedulerTest {

    private class Recorder(
        override val key: String,
        private val result: WorkResult = WorkResult.Success,
    ) : WorkHandler {
        val ran = mutableListOf<Map<String, String>>()

        override suspend fun run(input: Map<String, String>): WorkResult {
            ran += input
            return result
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val reminders = Recorder("reminders")
    private lateinit var scheduler: Scheduler
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(HandlerWorkerFactory { listOf(reminders) })
                .build(),
        )
        workManager = WorkManager.getInstance(context)
        scheduler = AndroidScheduler(context)
    }

    private fun stateOf(name: String) = workManager.getWorkInfosForUniqueWork(name).get().single().state

    private fun idOf(name: String) = workManager.getWorkInfosForUniqueWork(name).get().single().id

    @Test
    fun `a one-shot with a delay waits, and once the delay is met runs with its input`() {
        scheduler.schedule(
            WorkSpec(name = "trip-1", handler = "reminders", input = mapOf("trip" to "1"), delay = 2.hours),
        )

        assertEquals(WorkInfo.State.ENQUEUED, stateOf("trip-1"))
        assertTrue(reminders.ran.isEmpty())

        WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(idOf("trip-1"))

        assertEquals(WorkInfo.State.SUCCEEDED, stateOf("trip-1"))
        assertEquals(listOf(mapOf("trip" to "1")), reminders.ran)
    }

    @Test
    fun `constrained work runs when its constraints are met, not before`() {
        scheduler.schedule(
            WorkSpec(
                name = "sync",
                handler = "reminders",
                constraints = WorkConstraints(network = NetworkRequirement.Connected),
            ),
        )
        assertTrue(reminders.ran.isEmpty())

        WorkManagerTestInitHelper.getTestDriver(context)!!.setAllConstraintsMet(idOf("sync"))

        assertEquals(1, reminders.ran.size)
    }

    @Test
    fun `periodic work runs again each period`() {
        scheduler.schedule(WorkSpec(name = "refresh", handler = "reminders", repeat = 60.minutes))
        val id = idOf("refresh")
        val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
        // With no initial delay the first run is at once; each period met after that is another.
        val first = reminders.ran.size

        driver.setPeriodDelayMet(id)
        driver.setPeriodDelayMet(id)

        assertEquals(first + 2, reminders.ran.size)
        assertEquals(WorkInfo.State.ENQUEUED, stateOf("refresh"))
    }

    @Test
    fun `scheduling a name again replaces the work it named`() {
        scheduler.schedule(
            WorkSpec(name = "trip-1", handler = "reminders", input = mapOf("at" to "old"), delay = 2.hours),
        )
        scheduler.schedule(
            WorkSpec(name = "trip-1", handler = "reminders", input = mapOf("at" to "new"), delay = 2.hours),
        )

        WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(idOf("trip-1"))

        assertEquals(listOf(mapOf("at" to "new")), reminders.ran)
    }

    @Test
    fun `work that must keep what it has is not replaced`() {
        scheduler.schedule(
            WorkSpec(name = "trip-1", handler = "reminders", input = mapOf("at" to "first"), delay = 2.hours),
        )
        scheduler.schedule(
            WorkSpec(
                name = "trip-1",
                handler = "reminders",
                input = mapOf("at" to "second"),
                delay = 2.hours,
                replace = false,
            ),
        )

        WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(idOf("trip-1"))

        assertEquals(listOf(mapOf("at" to "first")), reminders.ran)
    }

    @Test
    fun `cancelling a name means it never runs`() {
        scheduler.schedule(WorkSpec(name = "trip-1", handler = "reminders", delay = 2.hours))

        scheduler.cancel("trip-1")

        assertEquals(WorkInfo.State.CANCELLED, stateOf("trip-1"))
        assertTrue(reminders.ran.isEmpty())
    }

    @Test
    fun `a handler's answer is the work's outcome`() {
        val failing = Recorder("failing", WorkResult.Failure)
        val retrying = Recorder("retrying", WorkResult.Retry)
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(HandlerWorkerFactory { listOf(failing, retrying) })
                .build(),
        )
        workManager = WorkManager.getInstance(context)

        scheduler.schedule(WorkSpec(name = "a", handler = "failing"))
        scheduler.schedule(WorkSpec(name = "b", handler = "retrying"))

        assertEquals(WorkInfo.State.FAILED, stateOf("a"))
        assertEquals(WorkInfo.State.ENQUEUED, stateOf("b"))
    }

    @Test
    fun `a handler nobody registered fails the work rather than crashing the app`() {
        scheduler.schedule(WorkSpec(name = "lost", handler = "nobody"))

        assertEquals(WorkInfo.State.FAILED, stateOf("lost"))
    }
}
