package com.example.androidproject1.core.ui.component.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Content that switches with a selection — a tab's panel, a wizard's step, a list for its empty
 * state — and travels rather than swaps: the new content slides in from the side of the higher
 * index and the old one slides out the other way, both fading, over `toggleMillis`. With
 * [slide] off it cross-fades, which is what two things that are not in an order do. The size
 * follows the content and nothing is clipped while it does.
 *
 * The one `AnimatedContent` a feature composes: a screen that switches on a bare `when`
 * pops, and one that names a spec is a screen that draws.
 */
@Composable
fun AppSwitcher(
    target: Int,
    modifier: Modifier = Modifier,
    slide: Boolean = true,
    content: @Composable (Int) -> Unit,
) {
    val motion = AppTheme.motion
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = {
            val spec = tween<Float>(motion.toggleMillis, easing = motion.toggleEasing)
            val transform: ContentTransform = if (slide) {
                // Forward is a slide from the end edge, back a slide from the start edge — the
                // direction a finger would have dragged the content in.
                val forward = targetState > initialState
                val offsets =
                    tween<androidx.compose.ui.unit.IntOffset>(motion.toggleMillis, easing = motion.toggleEasing)
                (slideInHorizontally(offsets) { if (forward) it else -it } + fadeIn(spec))
                    .togetherWith(slideOutHorizontally(offsets) { if (forward) -it else it } + fadeOut(spec))
            } else {
                fadeIn(spec).togetherWith(fadeOut(spec))
            }
            transform.using(SizeTransform(clip = false))
        },
        label = "switcher",
    ) { index ->
        content(index)
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md)) {
        AppSwitcher(target = 0) { index ->
            AppText(text = "Panel ${index + 1} of 3")
        }
        AppSwitcher(target = 1, slide = false) { index ->
            AppText(text = if (index == 0) "Results" else "Nothing here yet", role = TextRole.Secondary)
        }
    }
}
