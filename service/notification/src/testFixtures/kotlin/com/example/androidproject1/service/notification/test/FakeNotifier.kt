package com.example.androidproject1.service.notification.test

import com.example.androidproject1.service.notification.AppNotification
import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.NotificationPreferences
import com.example.androidproject1.service.notification.Notifier
import com.example.androidproject1.service.notification.PostResult

/**
 * A [Notifier] a test scripts: [result] is what every post answers, [allowed] whether a kind can
 * post, and [posted] and [cancelled] are what was asked for, in order.
 */
class FakeNotifier(
    var result: PostResult = PostResult.Posted,
    var allowed: Boolean = true,
    private val preferences: NotificationPreferences = FakeNotificationPreferences(),
) : Notifier {

    val posted = mutableListOf<AppNotification>()
    val cancelled = mutableListOf<Int>()

    override fun post(notification: AppNotification): PostResult {
        // The app's own switch, as the real one honours it: a kind turned off is dropped, not recorded.
        if (!preferences.isOn(notification.kind)) return PostResult.Off
        posted += notification
        return result
    }

    override fun cancel(id: Int) {
        cancelled += id
    }

    override fun canPost(kind: NotificationKind): Boolean = allowed && preferences.isOn(kind)
}
