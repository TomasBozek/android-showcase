package com.example.androidproject1.debug

import com.example.androidproject1.BuildConfig
import com.example.androidproject1.R
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.devtools.NotificationTester
import com.example.androidproject1.service.notification.AppNotification
import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.Notifier
import com.example.androidproject1.service.notification.PostResult

/**
 * The debug menu's "post a test notification", carrying a product deep link.
 *
 * In `:app` rather than in the feature because only this module knows the app's own URL scheme —
 * see `DeepLinks`. What posts, checks the permission and builds the tap's intent is the [Notifier];
 * this is the copy and the link. `:app` binds it into the debug menu's graph the same way
 * it binds `BuildInfo` and `OfflineSwitch`.
 *
 * In `main` rather than in the flavor source sets `DebugMenu` lives in: there is nothing per-flavor
 * about it, and `ApplicationModule` only reaches it from inside the branch that folds away in
 * `prod`, so R8 drops it there with everything else the debug menu names.
 */
class TestNotification(private val notifier: Notifier) : NotificationTester {

    override fun post(): Boolean = notifier.post(
        AppNotification(
            kind = NotificationKind.DevTest,
            id = NOTIFICATION_ID,
            title = R.string.debug_notification_title.toUiText(),
            text = R.string.debug_notification_text.toUiText(),
            deepLink = "${BuildConfig.APPLICATION_ID}://product/$PRODUCT_ID",
        ),
    ) == PostResult.Posted

    private companion object {

        const val NOTIFICATION_ID = 1

        /** A product the `dev` fixtures actually have, so the tap lands on a real screen. */
        const val PRODUCT_ID = "croissant"
    }
}
