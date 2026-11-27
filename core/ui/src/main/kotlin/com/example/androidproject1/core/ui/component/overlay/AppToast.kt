package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.status.feedback
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * What a toast is reporting. An error is the only one that does not dismiss itself. Declared in
 * `:service:core:ui`, since an `AppMessage` carries it from code that cannot name this module.
 */
typealias ToastTone = com.example.androidproject1.service.core.ui.event.ToastTone

/**
 * Confirmation of something that has already happened.
 *
 * It sits above every other layer, because it reports things that happened off-screen — an order
 * sent, a connection lost, a print that failed. Everything but an error disappears within five
 * seconds; an error waits to be read, so it is given [onDismiss] and draws a way to close it.
 *
 * With an action it is a snackbar — an offer — and the chrome draws it [ToastTone.Neutral]: the
 * tone belongs to a toast, which confirms, and a coloured offer reads as a verdict.
 */
@Composable
fun AppToast(
    message: String,
    modifier: Modifier = Modifier,
    tone: ToastTone = ToastTone.Neutral,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    // A toast is a message, so a toned one draws in a feedback role — the soft pair, never
    // the solid face of the button whose colour it used to borrow. Neutral stays the inverse
    // surface: it is the one that has nothing to say about how things went.
    val colors = AppTheme.colors
    val family = when (tone) {
        ToastTone.Neutral -> null
        ToastTone.Success -> colors.feedback.success
        ToastTone.Error -> colors.feedback.error
        ToastTone.Info -> colors.feedback.info
        ToastTone.Warning -> colors.feedback.warning
    }
    val background = family?.container ?: colors.surfaceInverse
    val foreground = family?.onContainer ?: colors.textOnInverse
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.lg)
            .background(background)
            .padding(
                horizontal = AppTheme.spacing.inset.lg,
                vertical = AppTheme.spacing.inset.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
    ) {
        AppText(
            text = message,
            role = TextRole.Body,
            color = foreground,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            ToastControl(label = actionLabel, color = foreground, onClick = onAction)
        }
        if (onDismiss != null) {
            ToastControl(label = stringResource(R.string.app_toast_dismiss), color = foreground, onClick = onDismiss)
        }
    }
}

@Composable
private fun ToastControl(label: String, color: Color, onClick: () -> Unit) {
    AppText(
        text = label,
        role = TextRole.Label,
        color = color,
        modifier = Modifier
            .clip(AppTheme.shapes.sm)
            .clickable(role = Role.Button, onClick = onClick)
            // A real control, so it gets a real target rather than the height of its own text.
            .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
            .padding(AppTheme.spacing.inset.sm),
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppToast(message = "Order placed", tone = ToastTone.Success)
    AppToast(message = "Item moved to your wishlist", actionLabel = "Undo", onAction = {})
    AppToast(message = "Printer not responding", tone = ToastTone.Error, onDismiss = {})
    AppToast(message = "Syncing in the background", tone = ToastTone.Info)
    AppToast(message = "Two items are low on stock", tone = ToastTone.Warning)
}
