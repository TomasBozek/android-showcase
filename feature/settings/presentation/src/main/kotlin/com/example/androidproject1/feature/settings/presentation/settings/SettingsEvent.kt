package com.example.androidproject1.feature.settings.presentation.settings

import com.example.androidproject1.feature.settings.domain.ThemePreference
import com.example.androidproject1.service.core.ui.event.UiEvent
import com.example.androidproject1.service.permission.DeclaredPermission

sealed interface SettingsEvent : UiEvent {

    data object ProfileClicked : SettingsEvent

    /** The copy icon beside the signed-in address. */
    data object CopyEmailClicked : SettingsEvent

    data object PermissionsClicked : SettingsEvent

    data object StorageClicked : SettingsEvent

    /** The Reminders switch: stored, and read by the notifier before anything is posted. */
    data class RemindersToggled(val on: Boolean) : SettingsEvent

    /** The app lock switch: stored, and read at the next cold start or long absence. */
    data class AppLockToggled(val on: Boolean) : SettingsEvent

    /**
     * What the manifest's permissions are now, read by the screen on every resume — the way
     * `SettingsPermissionsEvent.PermissionsRead` hands them over — so a permission granted or
     * refused elsewhere shows here without a restart.
     */
    data class PermissionsRead(val permissions: List<DeclaredPermission>) : SettingsEvent

    /** The row that opens the system's settings, shown once the permission was refused for good. */
    data object NotificationSettingsClicked : SettingsEvent

    data object AboutClicked : SettingsEvent

    /** Rate the app: the store's own sheet, asked for now. */
    data object RateClicked : SettingsEvent

    /** Write to the developer: a mail with the build filled in. */
    data object FeedbackClicked : SettingsEvent

    /** The support page, in a Custom Tab. */
    data object HelpClicked : SettingsEvent

    /** Put the tour back on: it runs again the next time Home is on screen. */
    data object ReplayTourClicked : SettingsEvent

    /**
     * Whether this build has a debug menu, reported by the destination.
     *
     * A build fact rather than something the user did, handed over the way
     * `SettingsPermissionsEvent.PermissionsRead` hands over what the system said: the ViewModel
     * stays the only owner of the state, and the screen still renders from it alone.
     */
    data class DebugMenuAvailable(val available: Boolean) : SettingsEvent

    data object DebugMenuClicked : SettingsEvent

    /** The language picker, a screen of this feature. */
    data object LanguageClicked : SettingsEvent

    /** A palette picked on the segmented control; stored, and applied at the root. */
    data class ThemeSelected(val theme: ThemePreference) : SettingsEvent

    /** The danger zone's header: open it, or fold it away again. */
    data object DangerZoneToggled : SettingsEvent

    data object LogoutClicked : SettingsEvent

    /** The danger zone's second row: asks first, and on confirm deletes the account. */
    data object DeleteAccountClicked : SettingsEvent
}
