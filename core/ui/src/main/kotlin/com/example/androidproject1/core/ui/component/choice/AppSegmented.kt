package com.example.androidproject1.core.ui.component.choice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.height
import com.example.androidproject1.core.ui.component.navigation.AppTabs
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Two to four short options that switch immediately.
 *
 * Longer labels or a badge mean [AppTabs] instead; this control has no room to grow and a fifth
 * option makes every one of them unreadable.
 *
 * [size] is the height of the whole control; the options divide the width between them whatever it
 * is, so a small segmented control is shorter rather than narrower.
 */
@Composable
fun AppSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: ControlSize = ControlSize.Medium,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion
    // The options share the width equally, so the thumb's place is the selected index times an
    // option's width — one fill laid behind the labels that slides from option to option over
    // `toggleMillis`, the labels recolouring over it on the same token. Laid out rather
    // than drawn, so its edges snap to the pixels the option's own edges do.
    BoxWithConstraints(
        modifier = modifier
            .clip(AppTheme.shapes.md)
            .background(colors.surfaceSunken)
            .border(1.dp, colors.borderStrong, AppTheme.shapes.md),
    ) {
        val optionWidth = if (options.isEmpty()) maxWidth else maxWidth / options.size
        val thumbX by animateDpAsState(
            targetValue = optionWidth * selectedIndex,
            animationSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
            label = "segmentThumb",
        )
        // Parent-sized for the height, then loosened so the width can be one option's: a fixed
        // constraint would otherwise win over `width`.
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = thumbX)
                .wrapContentWidth(Alignment.Start)
                .width(optionWidth)
                .background(colors.selection.bg),
        )
        Row(
            modifier = Modifier.selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                val labelColor by animateColorAsState(
                    targetValue = if (selected) colors.selection.label else colors.textPrimary,
                    animationSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
                    label = "segmentLabel",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = size.height)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        )
                        .padding(horizontal = AppTheme.spacing.inline.md),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = option,
                        role = TextRole.Label,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppSegmented(options = listOf("Today", "Week", "Month"), selectedIndex = 0, onSelect = {})
    AppSegmented(options = listOf("Percent", "Amount"), selectedIndex = 1, onSelect = {})
    AppSegmented(
        options = listOf("Today", "Week"),
        selectedIndex = 0,
        onSelect = {},
        size = ControlSize.Small,
    )
}
