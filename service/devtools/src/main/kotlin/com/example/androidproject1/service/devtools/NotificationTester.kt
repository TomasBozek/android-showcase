package com.example.androidproject1.service.devtools

/**
 * Posts the one notification a tester needs: the one that proves a tap-through lands where it
 * should.
 *
 * A seam rather than the thing itself, for the same reason `OfflineSwitch` is one. The notification
 * carries a `PendingIntent` for a deep link into this app, and only `:app` knows the app's own URL
 * scheme and which Activity receives it — a feature that built that intent would have to.
 */
public interface NotificationTester {

    /**
     * Posts it.
     *
     * @return `false` when the app may not post — the permission was refused, or the channel was
     * turned off in system settings. The caller says so rather than leaving the tester wondering
     * whether the notification was posted and missed.
     */
    public fun post(): Boolean

    /** For a build with nothing to post to. */
    public object Unsupported : NotificationTester {

        override fun post(): Boolean = false
    }
}
