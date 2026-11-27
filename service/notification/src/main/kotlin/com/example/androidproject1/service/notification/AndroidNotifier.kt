package com.example.androidproject1.service.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/** What only the app knows: the icon a notification's status bar entry wears. */
public data class NotificationConfig(@param:DrawableRes val smallIcon: Int)

/**
 * [Notifier] over `NotificationManagerCompat`.
 *
 * A tap's intent is the same `VIEW` an external app would send, **explicit about the package**, so
 * it cannot be answered by a chooser or by another app that registered the scheme, and it behaves
 * as `adb shell am start -d …` does. The `PendingIntent`s are immutable: nothing may rewrite a link
 * on its way through.
 */
public class AndroidNotifier(
    private val context: Context,
    private val config: NotificationConfig,
    private val preferences: NotificationPreferences,
) : Notifier {

    private val manager = NotificationManagerCompat.from(context)

    override fun post(notification: AppNotification): PostResult {
        // The app's own switch first: what the user turned off is dropped whatever else is granted.
        if (!preferences.isOn(notification.kind)) return PostResult.Off
        // Written out rather than folded into `canPost`, because this is the shape lint reads as a
        // guard for `notify` — `areNotificationsEnabled()` alone is not one, and neither is a
        // `runCatching` around the call.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return PostResult.PermissionRefused
        }
        if (!canPost(notification.kind)) return PostResult.Blocked

        val built = NotificationCompat.Builder(context, NotificationChannels.idOf(notification.kind))
            .setSmallIcon(config.smallIcon)
            .setContentTitle(notification.title.resolve(context))
            .setContentText(notification.text.resolve(context))
            .setContentIntent(linkIntent(notification.id, ACTION_SLOT_TAP, notification.deepLink))
            .setAutoCancel(notification.progress == null)
            .apply {
                notification.progress?.let { progress ->
                    // Ongoing and silent after the first: a download that beeps at every megabyte is not one.
                    setOngoing(true)
                    setOnlyAlertOnce(true)
                    setProgress(
                        (progress.max ?: 0L).toScaled(),
                        progress.value.toScaled(progress.max),
                        progress.max == null,
                    )
                }
                notification.group?.let(::setGroup)
                notification.actions.forEachIndexed { index, action ->
                    addAction(
                        0,
                        action.label.resolve(context),
                        linkIntent(notification.id, index + 1, action.deepLink),
                    )
                }
            }
            .build()

        return if (runCatching { manager.notify(notification.id, built) }.isSuccess) {
            PostResult.Posted
        } else {
            PostResult.Failed
        }
    }

    override fun cancel(id: Int): Unit = manager.cancel(id)

    override fun canPost(kind: NotificationKind): Boolean {
        if (!preferences.isOn(kind)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!manager.areNotificationsEnabled()) return false
        // A channel the user switched off in settings after granting the permission.
        val channel = manager.getNotificationChannel(NotificationChannels.idOf(kind))
        return channel == null || channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    /**
     * A tap's target. The request code is per notification and slot, so two notifications' links
     * never share a `PendingIntent`; `UPDATE_CURRENT` so posting one again replaces its link.
     */
    private fun linkIntent(id: Int, slot: Int, deepLink: String?): PendingIntent {
        val intent = if (deepLink != null) {
            Intent(Intent.ACTION_VIEW, deepLink.toUri()).setPackage(context.packageName)
        } else {
            requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)) {
                "no launcher activity to open"
            }
        }
        return PendingIntent.getActivity(
            context,
            id * SLOTS + slot,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** `setProgress` takes an `Int`: a 3 GB file's bytes do not fit, so both are scaled by the same factor. */
    private fun Long.toScaled(max: Long? = null): Int {
        val factor = ((max ?: this) / Int.MAX_VALUE) + 1
        return (this / factor).toInt()
    }

    private companion object {

        /** Slot 0 is the tap on the notification, 1.. its actions. */
        const val ACTION_SLOT_TAP = 0
        const val SLOTS = 8
    }
}
