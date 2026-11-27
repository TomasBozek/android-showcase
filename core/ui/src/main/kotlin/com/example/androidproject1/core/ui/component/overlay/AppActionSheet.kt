package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.content.AppIcon
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * A list of actions on the thing you long-pressed, from below.
 *
 * [AppMenu] is the same list anchored to a control — a "more" button in a top bar. This is the
 * one a phone shows when there is no control to anchor to: a long press on a row, a tap on a
 * card's corner. The rows are [MenuItem]s, so a screen that had a menu can offer the same
 * actions here without restating them; a destructive item sorts last behind a divider, as it
 * does in the menu, and a row is [AppTheme.density]'s touch target tall.
 *
 * A tap runs the item and then [onDismiss]: an action sheet is a question with one answer, and
 * it closes on the answer. On a wide window it is a centred card, as every [AppSheet] is.
 */
@Composable
fun AppActionSheet(
    items: List<MenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val ordered = items.sortedBy { it.destructive }
    AppSheet(onDismiss = onDismiss, modifier = modifier, title = title) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ordered.forEachIndexed { index, item ->
                if (item.destructive && index > 0 && !ordered[index - 1].destructive) {
                    AppDivider()
                }
                val color = when {
                    !item.enabled -> AppTheme.colors.textDisabled
                    item.destructive -> AppTheme.colors.destructive.bg
                    else -> AppTheme.colors.textPrimary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
                        .clickable(
                            enabled = item.enabled,
                            role = Role.Button,
                            onClick = {
                                item.onClick()
                                onDismiss()
                            },
                        )
                        .padding(vertical = AppTheme.spacing.inset.sm)
                        .then(if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
                ) {
                    if (item.icon != null) {
                        AppIcon(icon = item.icon, contentDescription = null, tint = color)
                    }
                    AppText(text = item.label, role = TextRole.Body, color = color)
                }
            }
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // A sheet draws in its own window; this shows the rows it would carry.
    Column {
        MenuRow(label = "Open", icon = Icons.Filled.Edit)
        MenuRow(label = "Share", icon = Icons.Filled.Share)
        AppDivider()
        MenuRow(label = "Delete", icon = Icons.Filled.Delete, destructive = true)
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, destructive: Boolean = false) {
    val color = if (destructive) AppTheme.colors.destructive.bg else AppTheme.colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
            .padding(vertical = AppTheme.spacing.inset.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
    ) {
        AppIcon(icon = icon, contentDescription = null, tint = color)
        AppText(text = label, role = TextRole.Body, color = color)
    }
}
