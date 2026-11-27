package com.example.androidproject1.core.ui.component.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.theme.AppTheme
import kotlin.math.roundToInt

/**
 * A figure that rolls when it changes — a badge's count, a stepper's value, a total, a
 * temperature — rather than swapping in one frame. Each character is a cell drawn by
 * [AppText] in the same [role], so a counter measures exactly as the text it replaces: a digit
 * that grows rolls up and one that shrinks rolls down over `toggleMillis`, anything that is not a
 * digit cross-fades. Every digit cell is as wide as the role's widest digit, so `9 → 10` moves
 * nothing and adds a cell. The first composition draws settled: rolling is for a change after
 * entry, never on it.
 *
 * A figure shrinks rather than wraps: cells never wrap, and a counter wider than the room it is
 * given scales down as one piece, the way [TextRole.Numeric] shrinks. It reads as one string to a
 * screen reader and a test, whatever the cells are doing.
 *
 * A golden of a counter at rest is deterministic: two recordings are byte-identical, and a cell
 * draws the pixels the plain text drew (both were measured). What looked like digits jittering
 * was the figure's box moving by a fraction of a pixel when a badge or a stepper first took a
 * counter — a real change under the compare threshold, reverted five times as noise. A
 * golden that moves by a few anti-aliased pixels after a counter changed is the new picture:
 * open it and commit it, never revert it.
 */
@Composable
fun AppCounter(
    text: String,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.Numeric,
    color: Color? = null,
) {
    val style = role.textStyle()
    val motion = AppTheme.motion
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // The widest digit in this style, once: the width every digit cell takes.
    val digitWidth = remember(style, density) {
        with(density) { DIGITS.maxOf { measurer.measure(it.toString(), style).size.width }.toDp() }
    }
    FitToWidth(modifier = modifier.clearAndSetSemantics { this.text = AnnotatedString(text) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            text.forEachIndexed { index, char ->
                AnimatedContent(
                    targetState = char,
                    modifier = if (char.isDigit()) Modifier.width(digitWidth) else Modifier,
                    contentAlignment = Alignment.Center,
                    transitionSpec = {
                        val fade = tween<Float>(motion.toggleMillis, easing = motion.toggleEasing)
                        val slide = tween<IntOffset>(motion.toggleMillis, easing = motion.toggleEasing)
                        if (targetState.isDigit() && initialState.isDigit()) {
                            // Up when the digit grows, down when it shrinks — the way a meter turns.
                            val up = targetState > initialState
                            (slideInVertically(slide) { if (up) it else -it } + fadeIn(fade))
                                .togetherWith(slideOutVertically(slide) { if (up) -it else it } + fadeOut(fade))
                        } else {
                            fadeIn(fade).togetherWith(fadeOut(fade))
                        }
                    },
                    label = "counterCell$index",
                ) { cell ->
                    AppText(text = cell.toString(), role = role, color = color, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Measures [content] at the width it wants and, when that is more than the room there is, scales
 * it down as one piece to fit — a figure shrinks rather than wraps or clips. The intrinsics say
 * the same, so a `FlowRow` sizing a row of cards by their tallest child counts the figure.
 */
@Composable
private fun FitToWidth(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier, measurePolicy = FitToWidthPolicy)
}

private object FitToWidthPolicy : MeasurePolicy {

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
        val scale = scaleFor(placeable.width, constraints.maxWidth)
        val width = (placeable.width * scale).roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height * scale).roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }

    // A figure fits any width by shrinking, so it asks a row for nothing and takes what it gets.
    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.first().maxIntrinsicWidth(Constraints.Infinity)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        scaledHeight(measurables.first(), width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        scaledHeight(measurables.first(), width)

    private fun scaledHeight(measurable: IntrinsicMeasurable, width: Int): Int {
        val wanted = measurable.maxIntrinsicWidth(Constraints.Infinity)
        val height = measurable.maxIntrinsicHeight(Constraints.Infinity)
        return (height * scaleFor(wanted, width)).roundToInt()
    }

    private fun scaleFor(wanted: Int, room: Int): Float =
        if (room != Constraints.Infinity && wanted > room && wanted > 0) room.toFloat() / wanted else 1f
}

private const val DIGITS = "0123456789"

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md)) {
        AppCounter(text = "1 248,00")
        AppCounter(text = "42", role = TextRole.DisplayLarge)
        AppCounter(text = "2 items", role = TextRole.Secondary)
    }
}
