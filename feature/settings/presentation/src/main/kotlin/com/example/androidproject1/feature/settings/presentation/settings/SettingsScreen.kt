package com.example.androidproject1.feature.settings.presentation.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.TabletPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.choice.AppSegmented
import com.example.androidproject1.core.ui.component.choice.AppSwitch
import com.example.androidproject1.core.ui.component.content.AppIcon
import com.example.androidproject1.core.ui.component.list.AppAccordion
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.layout.TabReselectEffect
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.settings.domain.ThemePreference
import com.example.androidproject1.feature.settings.presentation.R
import com.example.androidproject1.feature.settings.presentation.component.AccountHeader
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.permission.rememberDeclaredPermissions

@Composable
fun SettingsScreen(
    state: SettingsState,
    // Hoisted with a default, so the host's tab re-select and a test can reach it.
    scrollState: ScrollState = rememberScrollState(),
    onEvent: (SettingsEvent) -> Unit,
) {
    // A second tap on this tab pops to this root and scrolls it to the top.
    TabReselectEffect { scrollState.animateScrollTo(0) }
    // Read here, handed over as an event: a permission changes outside the app, so re-reading on
    // resume is the whole mechanism (as on the Permissions screen).
    val declared = rememberDeclaredPermissions()
    LaunchedEffect(declared) { onEvent(SettingsEvent.PermissionsRead(declared)) }
    AppScaffold(
        screenId = "SettingsScreen",
        // No up arrow: this is a tab root, and the bottom bar is what leaves it.
        topBar = { AppTopBar(title = stringResource(R.string.settings_title)) },
        // Rows run to the edge, as a list does; the one control that is not a row pads itself.
        contentPadding = false,
    ) {
        Column(
            // Scrollable: four sections are taller than a small phone's window, and a settings
            // entry that cannot be reached is not there.
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            // The account first: who this is, and the door to the profile that edits it.
            AppSectionHeader(
                title = stringResource(R.string.settings_account),
                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
            )
            AccountHeader(
                name = state.profileName,
                email = state.email,
                avatarUri = state.avatarUri,
                onClick = { onEvent(SettingsEvent.ProfileClicked) },
                onCopyEmail = { onEvent(SettingsEvent.CopyEmailClicked) },
            )

            AppSectionHeader(
                title = stringResource(R.string.settings_appearance),
                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
            )
            // Three options that switch immediately: a segmented control rather than a
            // switch, because `System` is a choice of its own and not the absence of one.
            AppSegmented(
                options = ThemePreference.entries.map { it.label() },
                selectedIndex = state.theme.ordinal,
                onSelect = { index ->
                    onEvent(SettingsEvent.ThemeSelected(ThemePreference.entries[index]))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.inset.lg)
                    .testTag("settings_themeTab"),
            )
            // Its own screen rather than a control here: a language name is long in every language
            // and the list grows with each `values-xx`. The row says what is chosen.
            AppListItem(
                headline = stringResource(R.string.settings_language),
                supporting = state.languageLabel?.resolve(),
                onClick = { onEvent(SettingsEvent.LanguageClicked) },
                modifier = Modifier.testTag("settings_languageButton"),
            )

            AppSectionHeader(
                title = stringResource(R.string.settings_notifications),
                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
            )
            AppSwitch(
                checked = state.remindersOn,
                onCheckedChange = { onEvent(SettingsEvent.RemindersToggled(it)) },
                label = stringResource(R.string.settings_reminders),
                supporting = stringResource(R.string.settings_reminders_supporting),
                modifier = Modifier
                    .padding(horizontal = AppTheme.spacing.inset.lg)
                    .testTag("settings_remindersSwitch"),
            )
            // Only once the system's dialog can no longer be shown: it is the one place left to grant it.
            if (state.notificationsRefusedForGood) {
                AppListItem(
                    headline = stringResource(R.string.settings_notification_settings),
                    supporting = stringResource(R.string.settings_notification_settings_supporting),
                    onClick = { onEvent(SettingsEvent.NotificationSettingsClicked) },
                    modifier = Modifier.testTag("settings_notificationSettingsButton"),
                )
            }

            AppSectionHeader(
                title = stringResource(R.string.settings_security),
                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
            )
            // Disabled rather than hidden with no screen lock: the line says what would let it on.
            AppSwitch(
                checked = state.appLockOn && state.appLockAvailable,
                onCheckedChange = { onEvent(SettingsEvent.AppLockToggled(it)) },
                label = stringResource(R.string.settings_app_lock_title),
                supporting = if (state.appLockAvailable) {
                    stringResource(R.string.settings_app_lock_supporting)
                } else {
                    stringResource(R.string.settings_app_lock_unavailable)
                },
                enabled = state.appLockAvailable,
                modifier = Modifier
                    .padding(horizontal = AppTheme.spacing.inset.lg)
                    .testTag("settings_appLockSwitch"),
            )

            AppSectionHeader(
                title = stringResource(R.string.settings_app),
                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
            )
            AppListItem(
                headline = stringResource(R.string.settings_permissions),
                onClick = { onEvent(SettingsEvent.PermissionsClicked) },
                leading = { AppIcon(icon = Icons.Filled.Lock, contentDescription = null) },
                modifier = Modifier.testTag("settings_permissionsButton"),
            )
            // A 100 MB download with progress, to show what a large file looks like here.
            AppListItem(
                headline = stringResource(R.string.settings_storage),
                onClick = { onEvent(SettingsEvent.StorageClicked) },
                leading = { AppIcon(icon = Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                modifier = Modifier.testTag("settings_storageButton"),
            )
            // It does not open anything: it puts the flag back and the tour starts on the next
            // Home, which is where its first step points.
            AppListItem(
                headline = stringResource(R.string.settings_tour),
                supporting = stringResource(R.string.settings_tour_supporting),
                onClick = { onEvent(SettingsEvent.ReplayTourClicked) },
                leading = { AppIcon(icon = Icons.Filled.PlayArrow, contentDescription = null) },
                modifier = Modifier.testTag("settings_tourButton"),
            )
            AppListItem(
                headline = stringResource(R.string.settings_rate),
                onClick = { onEvent(SettingsEvent.RateClicked) },
                leading = { AppIcon(icon = Icons.Filled.Star, contentDescription = null) },
                modifier = Modifier.testTag("settings_rateButton"),
            )
            AppListItem(
                headline = stringResource(R.string.settings_feedback),
                onClick = { onEvent(SettingsEvent.FeedbackClicked) },
                leading = { AppIcon(icon = Icons.Filled.Email, contentDescription = null) },
                modifier = Modifier.testTag("settings_feedbackButton"),
            )
            AppListItem(
                headline = stringResource(R.string.settings_help),
                onClick = { onEvent(SettingsEvent.HelpClicked) },
                leading = { AppIcon(icon = Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.testTag("settings_helpButton"),
            )
            AppListItem(
                headline = stringResource(R.string.settings_about),
                onClick = { onEvent(SettingsEvent.AboutClicked) },
                leading = { AppIcon(icon = Icons.Filled.Info, contentDescription = null) },
                modifier = Modifier.testTag("settings_aboutButton"),
            )
            // Only dev and staging have one; a prod build reports `false` and the entry
            // is not drawn. The component gallery lives behind it now.
            if (state.debugMenuEnabled) {
                AppListItem(
                    headline = stringResource(R.string.settings_debug_menu),
                    onClick = { onEvent(SettingsEvent.DebugMenuClicked) },
                    leading = { AppIcon(icon = Icons.Filled.Build, contentDescription = null) },
                    modifier = Modifier.testTag("settings_debugMenuButton"),
                )
            }

            // Folded by default: what is in here ends the session or worse, so it takes one tap
            // to see and a confirm dialog to do. The row is destructive; the dialog is the guard.
            AppAccordion(
                title = stringResource(R.string.settings_danger_zone),
                expanded = state.dangerZoneExpanded,
                onToggle = { onEvent(SettingsEvent.DangerZoneToggled) },
                modifier = Modifier
                    .padding(horizontal = AppTheme.spacing.inset.lg)
                    .testTag("settings_dangerZoneGroup"),
            ) {
                AppListItem(
                    headline = stringResource(R.string.settings_logout),
                    onClick = { onEvent(SettingsEvent.LogoutClicked) },
                    destructive = true,
                    leading = {
                        AppIcon(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = AppTheme.colors.destructive.bg,
                        )
                    },
                    modifier = Modifier.testTag("settings_logoutButton"),
                )
                // Below Log out, the thing that cannot be undone. The dialog is the guard.
                AppListItem(
                    headline = stringResource(R.string.settings_delete_account),
                    supporting = stringResource(R.string.settings_delete_account_supporting),
                    onClick = { onEvent(SettingsEvent.DeleteAccountClicked) },
                    destructive = true,
                    leading = {
                        AppIcon(
                            icon = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = AppTheme.colors.destructive.bg,
                        )
                    },
                    modifier = Modifier.testTag("settings_deleteAccountButton"),
                )
            }
        }
    }
}

/** The enum is the order the control draws in, so the labels are looked up rather than listed. */
@Composable
private fun ThemePreference.label(): String = stringResource(
    when (this) {
        ThemePreference.System -> R.string.settings_theme_system
        ThemePreference.Light -> R.string.settings_theme_light
        ThemePreference.Dark -> R.string.settings_theme_dark
    },
)

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    SettingsScreen(
        state = SettingsState.PREVIEW,
    ) {}
}

/** The danger zone open, so the destructive row is on record. */
@ScreenPreview
@Composable
private fun DangerZonePreview() = ThemedScreenPreview {
    SettingsScreen(
        state = SettingsState.PREVIEW_DANGER,
    ) {}
}

/** The list at a tablet's width: the rows run to their column's edge, and the column stops. */
@TabletPreview
@Composable
private fun TabletPreview() = ThemedScreenPreview {
    SettingsScreen(state = SettingsState.PREVIEW) {}
}
