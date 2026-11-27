package com.example.androidproject1.core.ui.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * How a list moves. A feature composes these and never names a spec: the durations are the
 * theme's, so a re-brand or reduced motion changes every list at once.
 *
 * [appAnimateItem] is `animateItem` on the tokens — an item that is added fades in over
 * `fadeMillis`, one that is removed fades out, and the rest slide into their new places over
 * `toggleMillis` — written as `modifier = appAnimateItem().testTag(…)` on the row of a keyed
 * `items {}`. It has to be the modifier of the item's own composable, which is why it is a
 * function on [LazyItemScope] rather than an extension on `Modifier`.
 */
@Composable
fun LazyItemScope.appAnimateItem(): Modifier {
    val motion = AppTheme.motion
    return Modifier.animateItem(
        fadeInSpec = tween(motion.fadeMillis, easing = motion.toggleEasing),
        placementSpec = tween(motion.toggleMillis, easing = motion.toggleEasing),
        fadeOutSpec = tween(motion.fadeMillis, easing = motion.toggleEasing),
    )
}

/**
 * An entrance for a thing among several — a door on Home, a favourite in its list: it fades from
 * nothing and lifts by `spacing.stack.sm` over `fadeMillis`, [index] steps after the first, so a
 * row of cards arrives as a run rather than as one block. The step is a fifth of the fade and the
 * index is capped at [STAGGER_CAP], so the ninth card waits no longer than the eighth. It runs once,
 * when the composable first enters, and is a no-op — no layer, no effect — when the fade is zero
 * or in inspection mode: a preview and its golden are the settled frame, the way they are for
 * `AppImage`, not the first frame of an entrance nobody can see.
 */
@Composable
fun Modifier.appStaggeredEntrance(index: Int): Modifier {
    val motion = AppTheme.motion
    if (motion.fadeMillis == 0 || LocalInspectionMode.current) return this
    val lift = with(LocalDensity.current) { AppTheme.spacing.stack.sm.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = motion.fadeMillis,
                delayMillis = index.coerceAtMost(STAGGER_CAP) * (motion.fadeMillis / STAGGER_STEPS),
                easing = motion.toggleEasing,
            ),
        )
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * lift
    }
}

/** The index past which every item arrives with the same delay. */
private const val STAGGER_CAP = 8

/** The delay between two neighbours, as a share of the fade: five steps in one fade. */
private const val STAGGER_STEPS = 5
