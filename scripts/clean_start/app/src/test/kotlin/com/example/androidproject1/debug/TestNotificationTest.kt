package com.example.androidproject1.debug

import com.example.androidproject1.BuildConfig
import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.PostResult
import com.example.androidproject1.service.notification.test.FakeNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestNotificationTest {

    private val notifier = FakeNotifier()

    @Test
    fun `it posts a dev test notification whose tap opens the debug menu`() {
        assertTrue(TestNotification(notifier).post())

        val posted = notifier.posted.single()
        assertEquals(NotificationKind.DevTest, posted.kind)
        assertEquals("${BuildConfig.APPLICATION_ID}://devmenu", posted.deepLink)
    }

    @Test
    fun `it says so when the app may not post`() {
        notifier.result = PostResult.PermissionRefused

        assertFalse(TestNotification(notifier).post())
    }
}
