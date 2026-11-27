package com.example.androidproject1.core.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layer 2 — elevation. Five levels and nothing between them.
 *
 * Each level has two parts: an **edge**, a hard bottom border that gives a key its body, and an
 * **ambient** shadow beneath it. Pressing shortens the edge, so the element drops by [PressTravel].
 * In dark the ambient does not read, so the edge carries the whole effect.
 *
 * The edge is drawn as a second background layer rather than through `Modifier.shadow`, because it
 * has to stay sharp: blurred, a key looks like a floating card instead of a pressed one.
 */
@Immutable
data class AppElevation(
    /** Surface, list row. */
    val level0: Dp = 0.dp,
    /** Card, tile. */
    val level1: Dp = 1.dp,
    /** Key, button. */
    val level2: Dp = 3.dp,
    /** Menu, floating action. */
    val level3: Dp = 6.dp,
    /** Dialog, sheet. */
    val level4: Dp = 10.dp,
)

/** How far a pressed element travels. */
val PressTravel: Dp = 3.dp

internal val LocalAppElevation = staticCompositionLocalOf { AppElevation() }

/**
 * A raised, pressable surface: [color] sitting on an [edge]-coloured body.
 *
 * Pass `pressed = true` and the body shrinks to a hairline, which is the travel effect — the
 * content moves down by [PressTravel] and the element looks depressed. A coloured element tints its
 * own edge; a neutral grey edge under a saturated button looks dirty, so pass the family's own
 * `edge` from [ActionColors].
 */
@Composable
fun Modifier.keySurface(
    color: Color,
    edge: Color,
    shape: Shape,
    edgeHeight: Dp = AppTheme.elevation.level2,
    pressed: Boolean = false,
): Modifier {
    val body by animateDpAsState(
        targetValue = if (pressed) edgeHeight - PressTravel else edgeHeight,
        animationSpec = tween(
            durationMillis = AppTheme.motion.pressMillis,
            easing = AppTheme.motion.pressEasing,
        ),
        label = "keySurfaceEdge",
    )
    return this
        .clip(shape)
        .background(edge)
        .padding(bottom = body.coerceAtLeast(0.dp))
        .clip(shape)
        .background(color)
}

/**
 * What a press feels like on a pressable that has no edge to shorten: a scale to
 * [PRESS_SCALE] while [interaction] is pressed, over `pressMillis` and back — so an outline
 * button, a plain icon button or a list row answers a finger the way a solid key does with its
 * travel. A solid key keeps [keySurface] and does not also scale: two cues for one press is a
 * button that wobbles. Under reduced motion the token is zero and the scale lands at once.
 */
@Composable
fun Modifier.pressFeedback(interaction: InteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESS_SCALE else 1f,
        animationSpec = tween(
            durationMillis = AppTheme.motion.pressMillis,
            easing = AppTheme.motion.pressEasing,
        ),
        label = "pressFeedback",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** How far a pressed, edgeless element shrinks: felt, not seen as a resize. */
private const val PRESS_SCALE = 0.97f
