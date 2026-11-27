package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppIcon
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Why the app wants a permission, said before it asks — what Android wants before a second ask
 * (`PermissionStatus.NeedsRationale`) — and, once the permission is refused for good, the way to
 * the app's settings, since the system dialog will not show again (`RefusedForGood`).
 *
 * An [AppDialog]: [title], the [icon] beside [message], then Not now and Continue — or, with
 * [refusedForGood], Open settings. Both actions report to the caller; the view model decides what
 * Continue means, so the dialog neither asks nor opens anything itself.
 *
 * @param continueTestTag the primary action's id, and [notNowTestTag] the other's: the caller's
 * [modifier] goes to the dialog, `AppConfirmDialog`'s precedent.
 */
@Composable
fun AppPermissionRationale(
    icon: ImageVector,
    title: String,
    message: String,
    refusedForGood: Boolean,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    continueTestTag: String? = null,
    notNowTestTag: String? = null,
) {
    AppDialog(
        title = title,
        onDismiss = onNotNow,
        modifier = modifier,
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
                verticalAlignment = Alignment.Top,
            ) {
                AppIcon(
                    icon = icon,
                    contentDescription = null,
                    size = AppTheme.icons.lg,
                    tint = AppTheme.colors.textPrimary,
                )
                AppText(text = message, role = TextRole.Body)
            }
        },
    ) {
        AppButton(
            label = stringResource(R.string.app_permission_not_now),
            onClick = onNotNow,
            kind = ButtonKind.Ghost,
            modifier = notNowTestTag?.let { Modifier.testTag(it) } ?: Modifier,
        )
        AppButton(
            label = stringResource(
                if (refusedForGood) R.string.app_permission_open_settings else R.string.app_permission_continue,
            ),
            onClick = onContinue,
            kind = ButtonKind.Confirm,
            modifier = continueTestTag?.let { Modifier.testTag(it) } ?: Modifier,
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // A dialog draws in its own window, so the preview shows what it would carry.
    AppText(text = "Allow notifications?", role = TextRole.TitleLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md)) {
        AppIcon(icon = Icons.Filled.Notifications, contentDescription = null, size = AppTheme.icons.lg)
        AppText(text = "We tell you when an order ships, and nothing else.", role = TextRole.Body)
    }
    AppButton(label = stringResource(R.string.app_permission_not_now), onClick = {}, kind = ButtonKind.Ghost)
    AppButton(label = stringResource(R.string.app_permission_continue), onClick = {}, kind = ButtonKind.Confirm)
}
