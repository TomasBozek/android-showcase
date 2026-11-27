package com.example.androidproject1.core.ui.component.choice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.component.navigation.AppToolbar
import com.example.androidproject1.core.ui.theme.AppTheme

/** A checkbox is on, off, or — only inside a group toggle — partly on. */
enum class CheckState { Off, On, Indeterminate }

/**
 * Selecting several things at once.
 *
 * The whole row is the target, not the 20 dp box, so the touch area clears the minimum without the
 * box having to grow. [CheckState.Indeterminate] belongs to a group's own toggle and nowhere else:
 * on a single item there is nothing for "partly" to mean.
 *
 * **An error always carries [errorText]**, the same contract [AppTextField] has and for the same
 * reason: a box outlined in red says something is wrong but not what, and to anyone who cannot
 * separate the hues it says nothing at all. The message goes under the row, where the next thing
 * read after the label is why it is refusing.
 *
 * [inverse] is for the one place a checkbox sits on a dark wash — the master on an [AppToolbar] —
 * and only moves the label onto [AppColors.textOnInverse]; the box carries its own colours.
 */
@Composable
fun AppCheckbox(
    checked: CheckState,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorText: String? = null,
    size: ControlSize = ControlSize.Medium,
    inverse: Boolean = false,
    /**
     * What a screen reader calls the box when [label] is blank — a row's checkbox in selection
     * mode, where the row already says the name. Ignored when there is a label: the
     * label is the name then, and two names is one too many.
     */
    contentDescription: String? = null,
) {
    val colors = AppTheme.colors
    val haptics = LocalHapticFeedback.current
    val on = checked != CheckState.Off
    val isError = errorText != null
    val box = if (size == ControlSize.Small) 18.dp else 22.dp
    val glyph = if (size == ControlSize.Small) 13.dp else 16.dp
    val fill by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = tween(AppTheme.motion.toggleMillis, easing = AppTheme.motion.toggleEasing),
        label = "checkboxFill",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
    ) {
        Row(
            modifier = Modifier
                // Both directions: a box with a one-word label laid out 43 dp wide.
                .defaultMinSize(minWidth = AppTheme.density.minTouchTarget, minHeight = AppTheme.density.minTouchTarget)
                .then(
                    if (label.isBlank() && contentDescription != null) {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    },
                )
                .toggleable(
                    value = on,
                    enabled = enabled,
                    role = Role.Checkbox,
                    // A tick is a state change, and is felt.
                    onValueChange = { next ->
                        haptics.performHapticFeedback(
                            if (next) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                        )
                        onCheckedChange(next)
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
        ) {
            Box(
                modifier = Modifier
                    .size(box)
                    .clip(AppTheme.shapes.sm)
                    .background(colors.confirm.bg.copy(alpha = fill))
                    .border(
                        width = if (on && !isError) 0.dp else 2.dp,
                        color = when {
                            isError -> colors.destructive.bg
                            enabled -> colors.borderStrong
                            else -> colors.border
                        },
                        shape = AppTheme.shapes.sm,
                    )
                    .alpha(if (enabled) 1f else 0.5f),
                contentAlignment = Alignment.Center,
            ) {
                when (checked) {
                    CheckState.On -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.confirm.label,
                        modifier = Modifier.size(glyph),
                    )
                    // A dash, not a second glyph: "some of the group", drawn rather than imported.
                    CheckState.Indeterminate -> Box(
                        modifier = Modifier
                            .size(width = 11.dp, height = 2.dp)
                            .background(colors.confirm.label),
                    )
                    CheckState.Off -> Unit
                }
            }
            AppText(
                text = label,
                role = TextRole.Body,
                color = when {
                    !enabled -> colors.textDisabled
                    inverse -> colors.textOnInverse
                    else -> colors.textPrimary
                },
            )
        }
        if (errorText != null) {
            AppText(text = errorText, role = TextRole.Label, color = colors.destructive.bg)
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppCheckbox(checked = CheckState.On, onCheckedChange = {}, label = "Email me a summary")
    AppCheckbox(checked = CheckState.Off, onCheckedChange = {}, label = "Send a weekly digest")
    AppCheckbox(checked = CheckState.Indeterminate, onCheckedChange = {}, label = "All items")
    AppCheckbox(
        checked = CheckState.Off,
        onCheckedChange = {},
        label = "Unavailable",
        enabled = false,
    )
    AppCheckbox(
        checked = CheckState.Off,
        onCheckedChange = {},
        label = "Accept the terms",
        errorText = "This has to be ticked before the order can be sent",
    )
    AppCheckbox(
        checked = CheckState.On,
        onCheckedChange = {},
        label = "Small, for a dense list",
        size = ControlSize.Small,
    )
}
