package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.content.AppImage
import com.example.androidproject1.core.ui.component.navigation.AppPager
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Pictures, full screen: swipe between them, pinch one to zoom and drag it about, close.
 *
 * [images] are whatever [AppImage] takes — a URL, a `Uri`, `null` for its placeholder — and the
 * viewer opens on [initialPage]. [AppPager] carries the swipe and the page dots; a picture zooms
 * between one and [MAX_ZOOM] times through `Modifier.transformable`, and the pager stops taking
 * swipes while it is zoomed, so a drag pans the picture instead of turning the page. Every page
 * starts at one, so a picture left zoomed is not still zoomed on the way back to it.
 *
 * It draws in a window of its own, over the system bars, which is why `OverlayScreenshotTest` has
 * its golden. That is also why it **fades** in rather than growing out of the thumbnail: a shared
 * element moves within one window's composition, and this is another window.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppImageViewer(
    images: List<Any?>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    /** What a screen reader calls the pictures; one description for the set. */
    contentDescription: String? = null,
) {
    val motion = AppTheme.motion
    val visible = remember { MutableTransitionState(false) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible.targetState = true }
    LaunchedEffect(closing, visible.isIdle) {
        if (closing && visible.isIdle) onClose()
    }
    val close = {
        closing = true
        visible.targetState = false
    }
    val pager =
        rememberPagerState(initialPage = initialPage.coerceIn(0, (images.size - 1).coerceAtLeast(0))) { images.size }
    var zoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(motion.sheetEnterMillis, easing = motion.toggleEasing)),
            exit = fadeOut(tween(motion.sheetExitMillis, easing = motion.toggleEasing)),
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.scrim)
                    // A window of its own does not inherit the activity's switch, so a flow could
                    // not find the close key by its id without this.
                    .semantics { testTagsAsResourceId = true },
            ) {
                AppPager(
                    pageCount = images.size,
                    state = pager,
                    userScrollEnabled = !zoomed,
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                ) { page ->
                    ZoomablePage(
                        image = images[page],
                        contentDescription = contentDescription,
                        onZoomChanged = { if (page == pager.currentPage) zoomed = it },
                        modifier = Modifier.testTag("imageViewer_item_$page"),
                    )
                }
                // On a raised disc of its own: whatever the picture is — dark, light, the loading
                // shimmer — the key reads against it, in both themes.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(AppTheme.spacing.inset.md)
                        .clip(CircleShape)
                        .background(AppTheme.colors.surfaceRaised),
                ) {
                    AppIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.app_image_viewer_close),
                        onClick = close,
                        modifier = Modifier.testTag("imageViewer_closeButton"),
                    )
                }
            }
        }
    }
}

/** One picture, zoomed and panned on its own; its state is the page's, so a new page starts at one. */
@Composable
private fun ZoomablePage(
    image: Any?,
    contentDescription: String?,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
        // Back at one there is nothing to pan to: the picture snaps home.
        offset = if (scale == 1f) Offset.Zero else offset + pan
        onZoomChanged(scale > 1f)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            // Panning only once zoomed: at one, a horizontal drag is the pager's.
            .transformable(state = state, canPan = { scale > 1f }),
        contentAlignment = Alignment.Center,
    ) {
        AppImage(
            model = image,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            shape = AppTheme.shapes.none,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

/** Five times: far enough to read a label on a photographed item, not so far it is all pixels. */
private const val MAX_ZOOM = 5f

private const val PREVIEW_ROWS = 4

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // The viewer draws in a window of its own; its golden is `OverlayScreenshotTest`'s. This is
    // the page it carries.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.density.listRowHeight * PREVIEW_ROWS)
            .background(AppTheme.colors.scrim),
    ) {
        AppImage(
            model = null,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            shape = AppTheme.shapes.none,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
