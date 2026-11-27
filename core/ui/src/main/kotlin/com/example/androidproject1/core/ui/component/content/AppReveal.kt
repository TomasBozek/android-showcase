package com.example.androidproject1.core.ui.component.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.navigation.AppToolbar
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Something that comes and goes on a condition — a floating action while nothing is selected, a
 * toolbar while something is — arriving and leaving on the theme's tokens rather than popping on
 * an `if`. The one `AnimatedVisibility` a feature composes.
 *
 * By default [content] fades and scales from 0.9 over `fadeMillis`, which is what a key or a
 * card does. With [slide] it slides in from the top over `sheetEnterMillis` and out over
 * `sheetExitMillis`, which is what a bar does: a selection toolbar revealed over the top bar
 * rather than swapped for it.
 */
@Composable
fun AppReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    slide: Boolean = false,
    content: @Composable () -> Unit,
) {
    val motion = AppTheme.motion
    val enter: EnterTransition
    val exit: ExitTransition
    if (slide) {
        enter = slideInVertically(tween(motion.sheetEnterMillis, easing = motion.toggleEasing)) { -it } +
            fadeIn(tween(motion.sheetEnterMillis, easing = motion.toggleEasing))
        exit = slideOutVertically(tween(motion.sheetExitMillis, easing = motion.toggleEasing)) { -it } +
            fadeOut(tween(motion.sheetExitMillis, easing = motion.toggleEasing))
    } else {
        enter = fadeIn(tween(motion.fadeMillis, easing = motion.toggleEasing)) +
            scaleIn(tween(motion.fadeMillis, easing = motion.toggleEasing), initialScale = REVEAL_SCALE)
        exit = fadeOut(tween(motion.fadeMillis, easing = motion.toggleEasing)) +
            scaleOut(tween(motion.fadeMillis, easing = motion.toggleEasing), targetScale = REVEAL_SCALE)
    }
    AnimatedVisibility(visible = visible, modifier = modifier, enter = enter, exit = exit) {
        content()
    }
}

/** Where a revealed thing starts and ends: small enough to read as arriving, not as loading. */
private const val REVEAL_SCALE = 0.9f

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md)) {
        AppReveal(visible = true) {
            AppButton(label = "Revealed", onClick = {})
        }
        AppReveal(visible = true, slide = true) {
            AppToolbar(title = "2 selected")
        }
    }
}
