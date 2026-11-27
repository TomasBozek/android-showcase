package com.example.androidproject1.core.ui.component.choice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.content.AppIcon
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.height
import com.example.androidproject1.core.ui.component.status.AppTag
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * A pill you can press: a filter, a recent search, a choice among a few.
 *
 * [AppTag] is the same shape and cannot be pressed — it says what something *is*. A chip says
 * what is *chosen*, so it toggles: `neutral.container` at rest, the selection role when
 * [selected], with a check mark that scales in over `toggleMillis` so the fill alone is never
 * what carries the state. `Role.Checkbox` semantics, because that is what a toggle is to a screen
 * reader; the pill is [ControlSize.Small] tall and its touch target the density's floor. A tap is
 * a state change, and is felt.
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion
    val haptics = LocalHapticFeedback.current
    val check by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
        label = "chipCheck",
    )
    val container = if (selected) colors.selection.container else colors.neutral.container
    val content = if (selected) colors.selection.onContainer else colors.neutral.onContainer
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
            .toggleable(
                value = selected,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { next ->
                    haptics.performHapticFeedback(
                        if (next) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                    )
                    onClick()
                },
            )
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(ControlSize.Small.height)
                .clip(AppTheme.shapes.pill)
                .background(container)
                .padding(horizontal = AppTheme.spacing.inset.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm),
        ) {
            // The check takes the icon's place when there is one, and appears from nothing when
            // there is not: either way the pill keeps its width while it toggles.
            if (check > 0f) {
                AppIcon(
                    icon = Icons.Filled.Check,
                    contentDescription = null,
                    size = AppTheme.icons.sm,
                    tint = content,
                    modifier = Modifier.scale(check),
                )
            } else if (leadingIcon != null) {
                AppIcon(icon = leadingIcon, contentDescription = null, size = AppTheme.icons.sm, tint = content)
            }
            AppText(text = label, role = TextRole.Label, color = content)
        }
    }
}

private const val DISABLED_ALPHA = 0.5f

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm)) {
        AppChip(label = "Fragile", selected = true, onClick = {})
        AppChip(label = "Lent out", selected = false, onClick = {})
        AppChip(label = "Insured", selected = false, onClick = {}, leadingIcon = Icons.Filled.Star)
        AppChip(label = "Sold", selected = false, onClick = {}, enabled = false)
    }
}
