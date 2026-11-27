package com.example.androidproject1.service.notification

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.notification.test.FakeNotificationPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidNotifierTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var notifier: AndroidNotifier
    private val preferences = FakeNotificationPreferences()

    private val reminder = AppNotification(
        kind = NotificationKind.Reminders,
        id = 7,
        title = UiText.Literal("Prague, tomorrow"),
        text = UiText.Literal("Pack the charger"),
        deepLink = "app://trip/7",
        actions = listOf(NotificationAction(UiText.Literal("Open the list"), "app://trip/7/list")),
        group = "trips",
    )

    @Before
    fun setUp() {
        NotificationChannels.create(context)
        notifier =
            AndroidNotifier(context, NotificationConfig(smallIcon = android.R.drawable.ic_dialog_info), preferences)
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `a reminder is posted to its channel with its link, its action and its group`() {
        val result = notifier.post(reminder)

        assertEquals(PostResult.Posted, result)
        val posted = shadowOf(manager).getNotification(7)
        assertEquals("reminders", posted.channelId)
        assertEquals("trips", posted.group)
        assertEquals("Prague, tomorrow", posted.extras.getCharSequence("android.title").toString())
        val tap = shadowOf(posted.contentIntent).savedIntent
        assertEquals("app://trip/7", tap.data.toString())
        assertEquals(context.packageName, tap.`package`)
        assertEquals("Open the list", posted.actions.single().title.toString())
        assertEquals("app://trip/7/list", shadowOf(posted.actions.single().actionIntent).savedIntent.data.toString())
    }

    @Test
    fun `a kind the user turned off in the app is dropped, and the others are not`() = kotlinx.coroutines.test.runTest {
        preferences.set(NotificationKind.Reminders, on = false)

        assertEquals(PostResult.Off, notifier.post(reminder))
        assertNull(shadowOf(manager).getNotification(7))
        assertFalse(notifier.canPost(NotificationKind.Reminders))
        assertTrue(notifier.canPost(NotificationKind.Downloads))

        preferences.set(NotificationKind.Reminders, on = true)
        assertEquals(PostResult.Posted, notifier.post(reminder))
    }

    @Test
    fun `posting the same id again replaces it, and cancel takes it away`() {
        notifier.post(reminder)
        notifier.post(reminder.copy(text = UiText.Literal("Charger and cable")))

        assertEquals(1, shadowOf(manager).size())
        assertEquals(
            "Charger and cable",
            shadowOf(manager).getNotification(7).extras.getCharSequence("android.text").toString(),
        )

        notifier.cancel(7)

        assertNull(shadowOf(manager).getNotification(7))
    }

    @Test
    fun `a notification with no link opens the app`() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        shadowOf(context.packageManager).addResolveInfoForIntent(
            launcher,
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = context.packageName
                    name = "MainActivity"
                }
            },
        )

        val result = notifier.post(reminder.copy(deepLink = null, actions = emptyList()))

        assertEquals(PostResult.Posted, result)
        val tap = shadowOf(shadowOf(manager).getNotification(7).contentIntent).savedIntent
        assertEquals(Intent.ACTION_MAIN, tap.action)
        assertEquals(context.packageName, tap.component?.packageName ?: tap.`package`)
    }

    @Test
    fun `nothing is posted, and canPost is false, with the permission refused`() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertFalse(notifier.canPost(NotificationKind.Reminders))
        assertEquals(PostResult.PermissionRefused, notifier.post(reminder))
        assertEquals(0, shadowOf(manager).size())
    }

    @Test
    fun `canPost is true with the permission held`() {
        assertTrue(notifier.canPost(NotificationKind.Reminders))
        assertTrue(notifier.canPost(NotificationKind.DevTest))
    }

    @Test
    fun `a channel the user switched off blocks its kind and no other`() {
        // What the user's switch does: the same channel, now at no importance.
        manager.createNotificationChannel(
            NotificationChannel("reminders", "Reminders", NotificationManager.IMPORTANCE_NONE),
        )

        assertFalse(notifier.canPost(NotificationKind.Reminders))
        assertEquals(PostResult.Blocked, notifier.post(reminder))
        assertTrue(notifier.canPost(NotificationKind.DevTest))
    }

    @Test
    fun `every kind has its own channel, created once and named`() {
        NotificationChannels.create(context)
        NotificationChannels.create(context)

        val names = manager.notificationChannels.associate { it.id to it.name.toString() }
        assertEquals("Reminders", names["reminders"])
        assertEquals("Developer tests", names["dev_test"])
        assertNull(names["general"])
    }
}
