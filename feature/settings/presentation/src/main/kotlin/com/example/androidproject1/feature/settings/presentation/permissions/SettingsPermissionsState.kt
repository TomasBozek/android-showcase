package com.example.androidproject1.feature.settings.presentation.permissions

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.settings.presentation.R
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText

/**
 * One permission the manifest declares.
 *
 * @property label what a person reads: "Notifications" for one the app knows, and the
 * last segment of [name] — `INTERNET` — for one it does not; the constant itself is the
 * supporting line either way.
 */
@Immutable
data class PermissionRow(
    val name: String,
    val label: UiText,
    val isGranted: Boolean,
)

/** What the rationale in front of the notifications ask says. */
enum class NotificationsRationale {

    /** Refused once: why, then Continue asks again. */
    Explain,

    /** Refused for good: why, then Continue opens the app's settings. */
    RefusedForGood,
}

@Immutable
data class SettingsPermissionsState(
    val permissions: List<PermissionRow>,
    /** Whether this app can post notifications, the one permission it asks for itself. */
    val canPostNotifications: Boolean,
    /** The rationale on screen, if any; the view model decides from the permission's status. */
    val rationale: NotificationsRationale? = null,
    /** The system dialog is to be shown: the screen asks, then reports `NotificationsAsked`. */
    val askNotifications: Boolean = false,
) {

    companion object {

        val PREVIEW = SettingsPermissionsState(
            permissions = listOf(
                PermissionRow(
                    name = "android.permission.INTERNET",
                    label = UiText.Literal("INTERNET"),
                    isGranted = true,
                ),
                PermissionRow(
                    name = "android.permission.POST_NOTIFICATIONS",
                    label = R.string.settings_permissions_label_notifications.toUiText(),
                    isGranted = false,
                ),
            ),
            canPostNotifications = false,
        )
    }
}
