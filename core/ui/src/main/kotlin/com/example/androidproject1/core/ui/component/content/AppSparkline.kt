package com.example.androidproject1.core.ui.component.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * A trend as one line: no axes, no labels, no grid — the shape of a week of temperatures, a
 * month of sales, read in a glance beside the figures that give it its numbers.
 *
 * The line is drawn in the brand accent, one row tall, scaled to its own minimum and maximum so a
 * flat week is a flat line and not a line lost at the top of the box. A [highlightIndex] puts a
 * dot on one point — today, this hour. A line says nothing to a screen reader, which is why the
 * [contentDescription] is required rather than defaulted: the caller has the figures and knows
 * what the line is of.
 *
 * **It draws itself in.** The path is traced from its start to its end over `screenMillis` and the
 * highlight dot pops once the line has reached it, so a trend arrives the way it is read —
 * left to right — rather than landing whole. Once per entry, keyed on `Unit`: a forecast that
 * refreshes under a finger redraws its line rather than wiping it and starting again.
 *
 * At zero (reduced motion) and in inspection mode the whole line is simply there, so a
 * preview and a golden are the settled frame the way they are for `appStaggeredEntrance` — a
 * golden of a half-drawn line is a golden of whichever frame the capture happened to land on.
 *
 * [values2] is a second series drawn in the same accent at [SECOND_SERIES_ALPHA] — a week's lows
 * under its highs — scaled against the two series' *combined* range, or lows colder than any high
 * would draw the pair crossing. It is ignored unless it is the same length as [values]: two series
 * of different lengths share no x axis, and there is no honest way to put them on one picture.
 *
 * [bars] is a 0–1 series drawn as columns under the line — a rain probability per hour. Values
 * outside 0–1 are clamped rather than refused: one out of range is a host's rounding error, not a
 * reason for a blank chart.
 */
@Composable
fun AppSparkline(
    values: List<Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
    values2: List<Float>? = null,
    bars: List<Float>? = null,
) {
    val color = AppTheme.colors.confirm.bg
    val screenMillis = AppTheme.motion.screenMillis
    val easing = AppTheme.motion.toggleEasing
    val still = screenMillis == 0 || LocalInspectionMode.current
    val drawn = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!still) drawn.animateTo(1f, tween(screenMillis, easing = easing))
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.density.listRowHeight)
            .semantics { this.contentDescription = contentDescription },
    ) {
        if (values.size < 2) return@Canvas
        // A second series only shares an x axis when it has the same number of points.
        val second = values2?.takeIf { it.size == values.size }
        val fraction = drawn.value
        val inset = DOT_RADIUS.toPx()
        val width = size.width - inset * 2
        val height = size.height - inset * 2

        // The bars are drawn first, under the line, so the trend stays the thing being read.
        bars?.takeIf { it.isNotEmpty() }?.let { series ->
            val barWidth = width / series.size * BAR_WIDTH
            series.forEachIndexed { index, raw ->
                val value = raw.coerceIn(0f, 1f)
                if (value <= 0f) return@forEachIndexed
                val centre = inset + width * index / (series.size - 1).coerceAtLeast(1)
                val barHeight = height * value * BAR_MAX_HEIGHT * fraction
                drawRect(
                    color = color.copy(alpha = BAR_ALPHA),
                    topLeft = Offset(centre - barWidth / 2f, size.height - inset - barHeight),
                    size = Size(barWidth, barHeight),
                )
            }
        }

        // Both series share one scale, or a low colder than every high draws them crossing.
        val min = minOf(values.min(), second?.min() ?: Float.MAX_VALUE)
        val max = maxOf(values.max(), second?.max() ?: -Float.MAX_VALUE)
        val span = (max - min).takeIf { it > 0f } ?: 1f
        fun point(series: List<Float>, index: Int): Offset = Offset(
            x = inset + width * index / (series.size - 1),
            y = inset + height * (1f - (series[index] - min) / span),
        )
        fun pathOf(series: List<Float>): Path = Path().apply {
            moveTo(point(series, 0).x, point(series, 0).y)
            for (index in 1 until series.size) lineTo(point(series, index).x, point(series, index).y)
        }

        val stroke = Stroke(width = STROKE.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun drawSeries(series: List<Float>, seriesColor: Color) {
            val path = pathOf(series)
            if (fraction >= 1f) {
                drawPath(path = path, color = seriesColor, style = stroke)
            } else if (fraction > 0f) {
                // `getSegment` walks the path by length rather than by point, so the line grows at
                // an even speed whatever the values do — a flat stretch does not race past a steep
                // one, and the two series stay level with each other as they draw.
                val measure = PathMeasure().apply { setPath(path, forceClosed = false) }
                val head = Path()
                measure.getSegment(0f, measure.length * fraction, head, startWithMoveTo = true)
                drawPath(path = head, color = seriesColor, style = stroke)
            }
        }
        second?.let { drawSeries(it, color.copy(alpha = SECOND_SERIES_ALPHA)) }
        drawSeries(values, color)

        highlightIndex?.takeIf { it in values.indices }?.let { index ->
            // The dot pops once the line has reached it, not before: a marker on a line that is
            // not there yet is a dot floating in a box.
            val reached = index.toFloat() / (values.size - 1)
            if (fraction >= reached) {
                drawCircle(color = color, radius = inset, center = point(values, index))
            }
        }
    }
}

/** Intrinsic to the line, like a divider's 1 dp: not a layout step, so not on the scale. */
private val STROKE = 2.dp
private val DOT_RADIUS = 4.dp

/** The second series reads as the pair's other half, not as a line of its own. */
private const val SECOND_SERIES_ALPHA = 0.45f

/** A column is a quiet ground under the line, never a thing competing with it. */
private const val BAR_ALPHA = 0.18f

/** How much of its slot a column fills, so columns read as separate rather than as a block. */
private const val BAR_WIDTH = 0.6f

/** Columns take the lower half, so a full one never reaches the line at its lowest. */
private const val BAR_MAX_HEIGHT = 0.5f

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppSparkline(
        values = listOf(12f, 14f, 13f, 17f, 19f, 18f, 22f),
        contentDescription = "Rising from 12 to 22 degrees over the week",
        highlightIndex = 4,
    )
    AppSparkline(
        values = listOf(22f, 21f, 17f, 15f, 15f, 12f, 9f),
        contentDescription = "Falling from 22 to 9 degrees over the week",
    )
    AppSparkline(
        values = listOf(16f, 16f, 16f, 16f, 16f, 16f, 16f),
        contentDescription = "A flat 16 degrees all week",
    )
    AppSparkline(
        values = listOf(18f, 19f, 22f, 20f, 17f, 16f, 19f),
        values2 = listOf(9f, 10f, 12f, 11f, 8f, 7f, 9f),
        contentDescription = "Daily highs and lows from 7 to 22 degrees over the week",
        highlightIndex = 0,
    )
    AppSparkline(
        values = listOf(12f, 13f, 15f, 18f, 20f, 19f, 16f, 14f),
        bars = listOf(0f, 0.1f, 0.3f, 0.8f, 1f, 0.4f, 0.1f, 0f),
        contentDescription = "Hourly temperature over the day, with rain probability",
    )
}
