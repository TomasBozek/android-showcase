package com.example.androidproject1.core.ui.component.status

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * The outline of content that is loading.
 *
 * It copies the shape of what is coming, and **a band of light travels across it** rather than the
 * whole outline blinking: a placeholder that flashes is harder to ignore than the wait it covers,
 * and one that never moves reads as content that has arrived and is blank. The sweep runs
 * `surfaceSunken` → `surfaceRaised` → `surfaceSunken` on `ambientMillis`, the one token for a loop
 * that never ends, which `AppIndeterminateProgress` reads too.
 *
 * A zero cycle is reduced motion: no transition is created and the outline sits at rest —
 * a branch on the token, never on the setting. Inspection mode takes the same branch, the way
 * `appStaggeredEntrance` and `AppImage` do: a golden is captured one frame in, and a golden of a
 * travelling band is a golden of wherever the band happened to be.
 */
@Composable
fun AppSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    shape: Shape = AppTheme.shapes.xs,
) {
    val ambientMillis = AppTheme.motion.ambientMillis
    val base = AppTheme.colors.surfaceSunken
    val crest = AppTheme.colors.surfaceRaised
    val travel = if (ambientMillis == 0 || LocalInspectionMode.current) {
        null
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val sweep by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            // Restart, not Reverse: the band travels one way and starts again, the way light moves
            // across a surface. Reversing it is the blink this component's own KDoc rules out.
            animationSpec = infiniteRepeatable(tween(ambientMillis), RepeatMode.Restart),
            label = "skeletonSweep",
        )
        sweep
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .then(
                if (travel == null) {
                    Modifier.background(base)
                } else {
                    // `drawBehind`, not `drawWithCache`: the offset changes every frame, so a
                    // cache block that reads it re-runs every frame and caches nothing — it only
                    // adds the invalidation machinery on top of the same allocation.
                    Modifier.drawBehind {
                        // The band enters at the left edge and leaves at the right, so some of it
                        // is on the outline at every moment of the cycle. Travelling from wholly
                        // before to wholly past leaves it off the element for most of the cycle —
                        // three frames in four on the emulator showed a skeleton standing still.
                        val band = size.width * BAND
                        val start = -band + (size.width + band) * travel
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(base, crest, base),
                                start = Offset(start, 0f),
                                end = Offset(start + band, 0f),
                            ),
                        )
                    }
                },
            ),
    )
}

/** How much of the width the lit band covers: wide enough to read as light, narrow enough to move. */
private const val BAND = 0.4f

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppSkeleton()
    AppSkeleton(height = 48.dp, shape = AppTheme.shapes.lg)
}
