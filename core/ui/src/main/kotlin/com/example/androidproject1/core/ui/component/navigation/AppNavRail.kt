package com.example.androidproject1.core.ui.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.status.AppBadge
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * The same destinations as [AppBottomNav], down the side, for the regular and expanded classes.
 *
 * Moving between size classes changes where navigation sits, never what is in it — someone who
 * learns the order on a phone finds the same order on a tablet.
 */
@Composable
fun AppNavRail(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .background(colors.surfaceRaised)
            .padding(vertical = AppTheme.spacing.inset.lg)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
                    .then(if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
            ) {
                // The pill arrives rather than switches: its colour, the icon's and the
                // label's cross over `toggleMillis`, and it scales in from PILL_START.
                val pillColor by animateColorAsState(
                    targetValue = if (selected) colors.selection.container else colors.surfaceRaised,
                    animationSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
                    label = "navPill",
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) colors.selection.onContainer else colors.textSecondary,
                    animationSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
                    label = "navIcon",
                )
                val labelColor by animateColorAsState(
                    targetValue = if (selected) colors.textPrimary else colors.textSecondary,
                    animationSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
                    label = "navLabel",
                )
                val pillScale by animateFloatAsState(
                    targetValue = if (selected) 1f else PILL_START,
                    animationSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
                    label = "navPillScale",
                )
                Box(contentAlignment = Alignment.TopEnd) {
                    Box {
                        // The pill is the icon's padded footprint, behind it, so it can scale
                        // without the icon moving.
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleX = pillScale
                                    scaleY = pillScale
                                }
                                .clip(AppTheme.shapes.pill)
                                .background(pillColor),
                        )
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier
                                .padding(
                                    horizontal = AppTheme.spacing.inline.md,
                                    vertical = AppTheme.spacing.inline.xs,
                                )
                                .size(AppTheme.icons.md),
                        )
                    }
                    AppBadge(count = item.badge)
                }
                AppText(text = item.label, role = TextRole.Label, color = labelColor)
            }
        }
    }
}

/** Where an arriving pill starts: small enough to read as arriving, not as a glitch. */
private const val PILL_START = 0.6f

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppNavRail(
        items = listOf(
            NavItem("Home", Icons.Filled.Home),
            NavItem("Orders", Icons.AutoMirrored.Filled.List, badge = 3),
        ),
        selectedIndex = 1,
        onSelect = {},
    )
}
