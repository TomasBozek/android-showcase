package com.example.androidproject1.feature.settings.presentation.settings

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.settings.domain.ThemePreference
import com.example.androidproject1.feature.settings.presentation.R
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText

@Immutable
data class SettingsState(
    val email: String?,
    /** The saved profile's name, for the account row; blank until one is put in. */
    val profileName: String? = null,
    val avatarUri: String? = null,
    /**
     * Whether this build has a debug menu. Not something a ViewModel can know: it is a
     * property of the build, so `:app` reports it through `SettingsDestination`.
     */
    val debugMenuEnabled: Boolean = false,
    /** Which palette the app draws in. `System` until the stored choice has been read. */
    val theme: ThemePreference = ThemePreference.DEFAULT,
    /** The stored language, as the Language row's supporting line; null until read. */
    val languageLabel: UiText? = null,
    /** The danger zone is closed by default: what it holds is meant to take a second look. */
    val dangerZoneExpanded: Boolean = false,
    /** The app's own switch for reminders; on until turned off. */
    val remindersOn: Boolean = true,
    /** The notification permission was refused for good: only the system's settings can grant it now. */
    val notificationsRefusedForGood: Boolean = false,
    /** The app lock's switch; off until turned on. */
    val appLockOn: Boolean = false,
    /** Whether the device has a biometric or a screen lock to prove it with; without one the switch is disabled. */
    val appLockAvailable: Boolean = true,
) {

    companion object {

        val PREVIEW = SettingsState(
            email = "ada@example.com",
            profileName = "Ada Lovelace",
            debugMenuEnabled = true,
            theme = ThemePreference.System,
            languageLabel = R.string.settings_language_system.toUiText(),
        )

        /** The danger zone open: what the log-out row looks like. */
        /** Reminders off and the permission refused for good: the two rows the Notifications section can add. */
        val PREVIEW_NOTIFICATIONS_REFUSED = PREVIEW.copy(remindersOn = false, notificationsRefusedForGood = true)

        val PREVIEW_DANGER = PREVIEW.copy(dangerZoneExpanded = true)
    }
}
