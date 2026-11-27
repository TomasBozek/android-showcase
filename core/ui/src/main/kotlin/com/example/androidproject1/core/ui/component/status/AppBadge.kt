package com.example.androidproject1.core.ui.component.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppCounter
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * A count over an icon or in a tab. Above 99 it stops being a number and becomes "99+".
 *
 * A count crossing zero scales in over `fadeMillis`, and a count going to zero scales out
 * showing its last value; a badge that is non-zero when it first composes draws settled.
 */
@Composable
fun AppBadge(count: Int, modifier: Modifier = Modifier) {
    // What the badge said last, so the exit has a number to show rather than an empty pill.
    var shown by remember { mutableIntStateOf(count) }
    if (count > 0) shown = count
    val motion = AppTheme.motion
    AnimatedVisibility(
        visible = count > 0,
        enter = scaleIn(tween(motion.fadeMillis, easing = motion.toggleEasing)) +
            fadeIn(tween(motion.fadeMillis, easing = motion.toggleEasing)),
        exit = scaleOut(tween(motion.fadeMillis, easing = motion.toggleEasing)) +
            fadeOut(tween(motion.fadeMillis, easing = motion.toggleEasing)),
    ) {
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                .clip(AppTheme.shapes.pill)
                .background(AppTheme.colors.destructive.bg)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppCounter(
                text = if (shown > 99) "99+" else shown.toString(),
                role = TextRole.Numeric,
                color = AppTheme.colors.destructive.label,
            )
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppBadge(count = 3)
    AppBadge(count = 42)
    AppBadge(count = 128)
}
