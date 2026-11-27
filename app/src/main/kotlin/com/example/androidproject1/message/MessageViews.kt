package com.example.androidproject1.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.R
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.overlay.AppDialog
import com.example.androidproject1.core.ui.component.status.AppBanner
import com.example.androidproject1.core.ui.component.status.BannerTone
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * A banner message in Home's header: the text, then its action if it has one and the key
 * that puts it away. An info tone, since a message is news rather than a warning.
 */
@Composable
fun MessageBanner(message: ServerMessage, onAction: () -> Unit, onDismiss: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
        modifier = Modifier.testTag("home_messageGroup"),
    ) {
        AppBanner(text = "${message.title} — ${message.body}", tone = BannerTone.Info)
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm)) {
            if (message.action != null) {
                AppButton(
                    label = stringResource(R.string.main_message_open),
                    onClick = onAction,
                    kind = ButtonKind.Outline,
                    size = ControlSize.Small,
                    modifier = Modifier.testTag("home_messageActionButton"),
                )
            }
            AppButton(
                label = stringResource(R.string.main_message_dismiss),
                onClick = onDismiss,
                kind = ButtonKind.Ghost,
                size = ControlSize.Small,
                modifier = Modifier.testTag("home_messageDismissButton"),
            )
        }
    }
}

/** A dialog message at start, over whatever is up; dismissing it by any means puts it away for good. */
@Composable
fun MessageDialog(message: ServerMessage, onAction: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        title = message.title,
        message = message.body,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("main_messageDialog"),
        actions = {
            AppButton(
                label = stringResource(R.string.main_message_dismiss),
                onClick = onDismiss,
                kind = ButtonKind.Ghost,
            )
            if (message.action != null) {
                AppButton(label = stringResource(R.string.main_message_open), onClick = onAction)
            }
        },
    )
}
