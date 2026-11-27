package com.example.androidproject1.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

/**
 * The channels, declared once — here — and created at start, before anything can post to one.
 *
 * Creating a channel is idempotent, so `App.onCreate` calls this every launch. A channel cannot be
 * changed once it is created, only its name: importance and the rest are the user's from then on.
 */
public object NotificationChannels {

    /** The channel a [kind] is posted to. */
    public fun idOf(kind: NotificationKind): String = when (kind) {
        NotificationKind.Reminders -> "reminders"
        NotificationKind.Downloads -> "downloads"
        NotificationKind.DevTest -> "dev_test"
    }

    public fun create(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                idOf(NotificationKind.Reminders),
                context.getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                idOf(NotificationKind.Downloads),
                context.getString(R.string.notification_channel_downloads),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                idOf(NotificationKind.DevTest),
                context.getString(R.string.notification_channel_dev_test),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        // The single "General" channel an earlier build made by hand; nothing posts to it now.
        NotificationManagerCompat.from(context).deleteNotificationChannel(LEGACY_GENERAL)
    }

    private const val LEGACY_GENERAL = "general"
}
