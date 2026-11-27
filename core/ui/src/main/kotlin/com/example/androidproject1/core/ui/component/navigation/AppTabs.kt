package com.example.androidproject1.core.ui.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.choice.AppSegmented
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.status.AppBadge
import com.example.androidproject1.core.ui.theme.AppTheme

/** One tab. [badge] above zero draws an [AppBadge]; below or at zero draws nothing. */
data class TabItem(val label: String, val badge: Int = 0)

/**
 * Switching content inside one screen.
 *
 * Unlike [AppSegmented] this takes a longer label and a count, and the selected tab is marked by a
 * rule underneath rather than a fill — a filled tab competes with the buttons in the content.
 */
@Composable
fun AppTabs(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion
    // The tabs share the width equally, so the rule's place is the selected index times a tab's
    // width — one indicator laid over the row's baseline that slides from tab to tab over
    // `toggleMillis`. Laid out rather than drawn, so its edges snap to the same pixels the
    // tab's own edges do and the resting frame is the one the goldens hold.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tabWidth = if (tabs.isEmpty()) maxWidth else maxWidth / tabs.size
        val indicatorX by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
            label = "tabIndicator",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalAlignment = Alignment.Bottom,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val labelColor by animateColorAsState(
                    targetValue = if (selected) colors.textPrimary else colors.textSecondary,
                    animationSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
                    label = "tabLabel",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
                            .padding(horizontal = AppTheme.spacing.inline.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm),
                    ) {
                        AppText(text = tab.label, role = TextRole.BodyLarge, color = labelColor)
                        AppBadge(count = tab.badge)
                    }
                    // The baseline every tab carries; the indicator slides over it.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(INDICATOR_HEIGHT)
                            .background(colors.border),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = indicatorX)
                .width(tabWidth)
                .height(INDICATOR_HEIGHT)
                .background(colors.selection.bg),
        )
    }
}

private val INDICATOR_HEIGHT = 2.dp

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppTabs(
        tabs = listOf(TabItem("All", badge = 4), TabItem("Unread"), TabItem("Archived")),
        selectedIndex = 0,
        onSelect = {},
    )
}
