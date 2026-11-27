package com.example.androidproject1.service.notification

import com.example.androidproject1.service.core.ui.text.UiText

/**
 * What the app posts to the notification shade. The app has one kind a user can meet — [Reminders]
 * — and one only a developer does.
 *
 * The kind is the channel: what the user sees in system settings and can turn off on its own.
 */
public enum class NotificationKind {
    Reminders,

    /** A file coming down: quiet, and it stays until it is done. */
    Downloads,
    DevTest,
}

/**
 * One notification. [id] is the caller's, and posting the same one again replaces it — which is
 * how "the reminder for trip 7" stays one notification however many times it is raised.
 *
 * A tap opens [deepLink], a link into this app (`scheme://host/path`) that `MainActivity` handles
 * like one from outside; `null` opens the app. Each of [actions] opens its own link. Notifications
 * with the same [group] are stacked under it.
 */
public data class AppNotification(
    val kind: NotificationKind,
    val id: Int,
    val title: UiText,
    val text: UiText,
    val deepLink: String? = null,
    val actions: List<NotificationAction> = emptyList(),
    val group: String? = null,
    val progress: NotificationProgress? = null,
)

/**
 * A notification that is a job going on: [value] of [max], or no [max] while the total is not known.
 * One with a progress cannot be swiped away and does not make a sound each time it moves; posting
 * again with a new value updates it in place, and posting without one turns it into an ordinary one.
 */
public data class NotificationProgress(val value: Long, val max: Long?)

public data class NotificationAction(val label: UiText, val deepLink: String)

/** Why a post did or did not happen, so a caller can say something better than "it did not work". */
public enum class PostResult {
    Posted,

    /** The user turned this kind off in the app's own settings ([NotificationPreferences]). */
    Off,

    /** `POST_NOTIFICATIONS` is not held (API 33+). */
    PermissionRefused,

    /** The permission is held but the app's notifications, or this kind's channel, are off. */
    Blocked,
    Failed,
}

public interface Notifier {

    public fun post(notification: AppNotification): PostResult

    public fun cancel(id: Int)

    /** Whether a [kind] notification would be shown right now. Read it, do not cache it: it changes in settings. */
    public fun canPost(kind: NotificationKind): Boolean
}
