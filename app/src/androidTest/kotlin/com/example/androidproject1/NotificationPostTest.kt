package com.example.androidproject1

import android.Manifest
import android.app.NotificationManager
import android.service.notification.StatusBarNotification
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.notification.AppNotification
import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.Notifier
import com.example.androidproject1.service.notification.PostResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * A notification the app's own [Notifier] posts is one the system shows, on its channel — the half
 * Robolectric's shadow only pretends to.
 */
@RunWith(AndroidJUnit4::class)
class NotificationPostTest {

    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier = GlobalContext.get().get<Notifier>()

    @After
    fun tearDown() = notifier.cancel(ID)

    @Test
    fun aPostedNotificationIsActiveAndACancelledOneIsGone() {
        val result = notifier.post(
            AppNotification(
                kind = NotificationKind.DevTest,
                id = ID,
                title = "Instrumented".toUiText(),
                text = "Posted by NotificationPostTest".toUiText(),
            ),
        )

        assertEquals(PostResult.Posted, result)
        // The system service takes a post asynchronously, so it is listed a moment later.
        val posted = awaitActive(present = true)
        assertTrue("the system lists it", posted != null)
        assertTrue(
            "on a channel the app created",
            manager.getNotificationChannel(posted!!.notification.channelId) != null,
        )

        notifier.cancel(ID)

        assertFalse("a cancelled one is gone", awaitActive(present = false) != null)
    }

    /** The notification once it is [present] (or `null` once it is not), within [WAIT_MS]. */
    private fun awaitActive(present: Boolean): StatusBarNotification? {
        val deadline = System.currentTimeMillis() + WAIT_MS
        while (true) {
            val active = manager.activeNotifications.singleOrNull { it.id == ID }
            if ((active != null) == present || System.currentTimeMillis() > deadline) return active
            Thread.sleep(STEP_MS)
        }
    }

    private companion object {

        const val ID = 0x1A2B
        const val WAIT_MS = 5_000L
        const val STEP_MS = 50L
    }
}
