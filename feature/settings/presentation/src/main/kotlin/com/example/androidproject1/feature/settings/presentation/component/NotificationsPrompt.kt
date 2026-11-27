package com.example.androidproject1.feature.settings.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppCard
import com.example.androidproject1.feature.settings.presentation.R

/**
 * The one permission this app asks for itself, asked for where the user came to look at
 * permissions — rather than thrown at them on first launch, which is what gets it denied.
 *
 * Allow only reports: the view model reads the permission's status and decides whether to ask,
 * explain first, or send the user to the app's settings.
 */
@Composable
fun NotificationsPrompt(onAllow: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        AppText(
            text = stringResource(R.string.settings_permissions_notifications_title),
            role = TextRole.Title,
        )
        AppText(
            text = stringResource(R.string.settings_permissions_notifications_message),
            role = TextRole.Secondary,
        )
        AppButton(
            label = stringResource(R.string.settings_permissions_allow),
            onClick = onAllow,
            kind = ButtonKind.Ghost,
            modifier = Modifier.testTag("settingsPermissions_allowButton"),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    NotificationsPrompt(onAllow = {})
}
