package com.example.androidproject1.core.ui.component.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.status.AppSkeleton
import com.example.androidproject1.core.ui.theme.AppTheme
import kotlin.math.max
import kotlin.math.min

/**
 * Which part of a picture a square crop takes, in fractions of the picture's own width and height,
 * so the caller crops the full-size file whatever size it was drawn at: `left` and `top` are the
 * square's corner, `width` and `height` its side over each dimension.
 */
@Immutable
data class ImageCrop(val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * A picture under a square frame, panned and pinched until the frame holds the part wanted — an
 * avatar, a cover.
 *
 * The frame is the component: square, and as large as the space it is given allows. The picture starts covering
 * it, centred, and never shows an edge inside it — a pan stops at the picture's edge and a pinch
 * zooms in up to [maxZoom] times and never out past covering. Every change is reported through
 * [onCropChange] as an [ImageCrop], first the centred one; the component crops nothing itself.
 */
@Composable
fun AppImageCropper(
    image: Painter,
    onCropChange: (ImageCrop) -> Unit,
    modifier: Modifier = Modifier,
    maxZoom: Float = DEFAULT_MAX_ZOOM,
) {
    val picture = image.intrinsicSize
    val report by rememberUpdatedState(onCropChange)
    var frame by remember { mutableFloatStateOf(0f) }
    var zoom by remember(image) { mutableFloatStateOf(1f) }
    // The picture's top-left corner in the frame; null until the frame has a size to centre in.
    var corner by remember(image) { mutableStateOf<Offset?>(null) }

    fun scale() = if (picture.minDimension > 0f) frame / picture.minDimension * zoom else 0f

    fun clamp(offset: Offset): Offset {
        val s = scale()
        return Offset(
            offset.x.coerceIn(min(0f, frame - picture.width * s), 0f),
            offset.y.coerceIn(min(0f, frame - picture.height * s), 0f),
        )
    }

    val transform = rememberTransformableState { zoomChange, pan, _ ->
        val current = corner ?: return@rememberTransformableState
        val before = scale()
        zoom = (zoom * zoomChange).coerceIn(1f, max(1f, maxZoom))
        // About the frame's centre, so the part in the middle stays there while it grows.
        val centre = Offset(frame / 2, frame / 2)
        val ratio = if (before > 0f) scale() / before else 1f
        corner = clamp(centre - (centre - current) * ratio + pan)
    }

    LaunchedEffect(frame, picture) {
        if (frame > 0f && corner == null && picture.minDimension > 0f) {
            val s = scale()
            corner = Offset((frame - picture.width * s) / 2, (frame - picture.height * s) / 2)
        }
    }
    LaunchedEffect(corner, zoom) {
        val at = corner ?: return@LaunchedEffect
        val s = scale()
        if (s <= 0f) return@LaunchedEffect
        val side = frame / s
        report(
            ImageCrop(
                // `+ 0f` turns the -0.0 a corner at the origin negates to into 0.0.
                left = -at.x / s / picture.width + 0f,
                top = -at.y / s / picture.height + 0f,
                width = side / picture.width,
                height = side / picture.height,
            ),
        )
    }

    val edge = AppTheme.colors.borderStrong
    Canvas(
        // Square, as large as the space allows: the width when there is height for it, the height
        // when there is not — so buttons under it keep theirs.
        modifier = modifier
            .aspectRatio(1f)
            .clipToBounds()
            .onSizeChanged { frame = it.width.toFloat() }
            .transformable(transform),
    ) {
        val at = corner
        val s = scale()
        if (at != null && s > 0f) {
            translate(at.x, at.y) {
                with(image) { draw(Size(picture.width * s, picture.height * s)) }
            }
        }
        drawRect(color = edge, style = Stroke(width = FRAME_WIDTH.toPx()))
    }
}

/**
 * The same, for a picture still to be loaded — a file or content URI, as [AppImage] takes. A
 * skeleton holds the frame until the picture is there, because there is nothing to crop before.
 */
@Composable
fun AppImageCropper(
    model: Any?,
    onCropChange: (ImageCrop) -> Unit,
    modifier: Modifier = Modifier,
    maxZoom: Float = DEFAULT_MAX_ZOOM,
) {
    val painter = rememberAsyncImagePainter(model)
    when (painter.state.collectAsState().value) {
        is AsyncImagePainter.State.Success ->
            AppImageCropper(image = painter, onCropChange = onCropChange, modifier = modifier, maxZoom = maxZoom)
        else -> AppSkeleton(modifier = modifier.aspectRatio(1f))
    }
}

private const val DEFAULT_MAX_ZOOM = 4f

// Intrinsic to this component: a frame line, not a spacing.
private val FRAME_WIDTH = 2.dp

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // A wide picture drawn here, so the golden needs no file: three bands, the middle one framed.
    val bands = listOf(AppTheme.colors.surfaceSunken, AppTheme.colors.borderStrong, AppTheme.colors.surfaceSunken)
    val picture = remember(bands) {
        ImageBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT).also { bitmap ->
            val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
            val paint = androidx.compose.ui.graphics.Paint()
            bands.forEachIndexed { index, colour ->
                paint.color = colour
                val third = PREVIEW_WIDTH / bands.size.toFloat()
                canvas.drawRect(third * index, 0f, third * (index + 1), PREVIEW_HEIGHT.toFloat(), paint)
            }
        }
    }
    AppImageCropper(image = BitmapPainter(picture), onCropChange = {})
}

private const val PREVIEW_WIDTH = 300
private const val PREVIEW_HEIGHT = 200
