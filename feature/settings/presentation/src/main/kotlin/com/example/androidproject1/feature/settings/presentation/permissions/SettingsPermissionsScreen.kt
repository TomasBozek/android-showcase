package com.example.androidproject1.feature.settings.presentation.permissions

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.overlay.AppPermissionRationale
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.settings.presentation.R
import com.example.androidproject1.feature.settings.presentation.component.NotificationsPrompt
import com.example.androidproject1.feature.settings.presentation.component.PermissionListItem
import com.example.androidproject1.service.permission.rememberDeclaredPermissions
import com.example.androidproject1.service.permission.rememberPermissionRequest

@Composable
fun SettingsPermissionsScreen(
    state: SettingsPermissionsState,
    onEvent: (SettingsPermissionsEvent) -> Unit,
) {
    // Read here rather than in the ViewModel, and handed over as an event: a permission changes
    // outside the app, so re-reading on resume is the whole mechanism. See SettingsPermissionsEvent.
    val declared = rememberDeclaredPermissions()
    LaunchedEffect(declared) {
        onEvent(SettingsPermissionsEvent.PermissionsRead(declared))
    }
    // The system dialog is an activity result, so the screen shows it — when the view model says to.
    val notifications = rememberPermissionRequest(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(state.askNotifications) {
        if (state.askNotifications) {
            notifications.request()
            onEvent(SettingsPermissionsEvent.NotificationsAsked)
        }
    }
    state.rationale?.let { rationale ->
        val refused = rationale == NotificationsRationale.RefusedForGood
        AppPermissionRationale(
            icon = Icons.Filled.Notifications,
            title = if (refused) {
                stringResource(R.string.settings_permissions_refused_title)
            } else {
                stringResource(R.string.settings_permissions_rationale_title)
            },
            message = if (refused) {
                stringResource(R.string.settings_permissions_refused_message)
            } else {
                stringResource(R.string.settings_permissions_rationale_message)
            },
            refusedForGood = refused,
            onContinue = { onEvent(SettingsPermissionsEvent.RationaleContinueClicked) },
            onNotNow = { onEvent(SettingsPermissionsEvent.RationaleNotNowClicked) },
            modifier = Modifier.testTag("settingsPermissions_rationaleDialog"),
            continueTestTag = "settingsPermissions_continueButton",
            notNowTestTag = "settingsPermissions_notNowButton",
        )
    }
    AppScaffold(
        screenId = "SettingsPermissionsScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_permissions_title),
                onNavigateUp = { onEvent(SettingsPermissionsEvent.NavigateUpClicked) },
                navigateUpTestTag = "settingsPermissions_upButton",
            )
        },
        contentPadding = false,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (!state.canPostNotifications) {
                item {
                    NotificationsPrompt(
                        onAllow = { onEvent(SettingsPermissionsEvent.AllowNotificationsClicked) },
                        modifier = Modifier.padding(AppTheme.spacing.inset.lg),
                    )
                }
            }
            items(state.permissions, key = { it.name }) { row ->
                PermissionListItem(row = row, modifier = appAnimateItem())
                AppDivider(modifier = appAnimateItem())
            }
            item {
                AppButton(
                    label = stringResource(R.string.settings_permissions_open_settings),
                    onClick = { onEvent(SettingsPermissionsEvent.OpenAppSettingsClicked) },
                    kind = ButtonKind.Outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spacing.inset.lg)
                        .testTag("settingsPermissions_openSettingsButton"),
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    SettingsPermissionsScreen(
        state = SettingsPermissionsState.PREVIEW,
    ) {}
}
