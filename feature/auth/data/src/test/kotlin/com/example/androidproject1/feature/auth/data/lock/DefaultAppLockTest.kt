package com.example.androidproject1.feature.auth.data.lock

import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.lifecycle.Foreground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The app lock's rule: off until switched on; switched on, it holds at a cold start and
 * again after five minutes away, and `unlocked()` lets the user in. A real DataStore, a scripted
 * foreground and a clock the test moves.
 */
@RunWith(RobolectricTestRunner::class)
class DefaultAppLockTest {

    private val provider =
        DataStoreProvider(ApplicationProvider.getApplicationContext(), name = "app_lock_test_${System.nanoTime()}")
    private val inFront = MutableStateFlow(true)
    private val foreground = object : Foreground {
        override val isForeground = inFront
    }
    private val clock = MovableClock()

    private fun TestScope.lock() = DefaultAppLock(
        dataStoreProvider = provider,
        foreground = foreground,
        clock = clock,
        scope = backgroundScope,
    )

    private suspend fun DefaultAppLock.lockedNow(): Boolean = locked.first { it != null }!!

    @Test
    fun `off by default, so nothing is ever held`() = runTest {
        val lock = lock()

        assertFalse(lock.enabled.first())
        assertFalse(lock.lockedNow())
        away(Duration.ofMinutes(30))
        assertFalse(lock.lockedNow())
    }

    @Test
    fun `nothing is known until the setting is read`() = runTest {
        // `null` rather than a guess, so a lock screen never flashes for someone who has it off.
        assertNull(DefaultAppLock(provider, foreground, clock, backgroundScope).locked.value)
    }

    @Test
    fun `on, a cold start is held until unlocked`() = runTest {
        lock().setEnabled(true)
        val coldStart = lock()

        assertTrue(coldStart.lockedNow())
        coldStart.unlocked()
        assertFalse(coldStart.lockedNow())
    }

    @Test
    fun `switching it on does not lock out the user who just did`() = runTest {
        val lock = lock()

        lock.setEnabled(true)

        assertTrue(lock.enabled.first())
        assertFalse(lock.lockedNow())
    }

    @Test
    fun `five minutes away holds it again, four fifty-nine does not`() = runTest {
        val lock = lock()
        lock.setEnabled(true)

        away(Duration.ofMinutes(5).minusSeconds(1))
        assertFalse("4:59 away", lock.lockedNow())

        away(Duration.ofMinutes(5))
        assertTrue("5:00 away", lock.lockedNow())
    }

    @Test
    fun `switched off, a held app is let in`() = runTest {
        lock().setEnabled(true)
        val lock = lock()
        assertTrue(lock.lockedNow())

        lock.setEnabled(false)

        assertFalse(lock.lockedNow())
        assertEquals(false, lock.enabled.first())
    }

    /** Into the background and back. The scheduler runs between, as minutes would: a StateFlow conflates two writes in a row. */
    private fun TestScope.away(duration: Duration) {
        inFront.value = false
        testScheduler.runCurrent()
        clock.now = clock.now.plus(duration)
        inFront.value = true
        testScheduler.runCurrent()
    }

    private class MovableClock(var now: Instant = Instant.parse("2026-10-01T12:00:00Z")) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }
}
