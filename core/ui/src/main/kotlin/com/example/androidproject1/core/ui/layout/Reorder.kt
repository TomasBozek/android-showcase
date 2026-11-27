package com.example.androidproject1.core.ui.layout

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Dragging a row into a new place.
 *
 * First-party, and forty lines: a drag between rows is one pass over the list's own
 * `layoutInfo` asking which row the pointer is now over. What it is not is a library: a third-party
 * reorder would be the first dependency in the release build that is none of Google, JetBrains or
 * androidx.
 *
 * Three pieces, and a list needs all three:
 *
 * - [rememberReorderState] holds the gesture and owns the `LazyListState` the list is given.
 * - [Modifier.appReorderItem] goes on each row: it lifts the dragged one so it stays under the
 *   finger while the rest slide past it.
 * - `AppReorder` is the handle the gesture starts on, and [Modifier.appReorderHandle] is what it
 *   is built from — never the row itself, because `AppListItem` already drags sideways for its
 *   swipe keys and two gestures on one surface is how a delete becomes a reorder.
 *
 * **The caller owns the order.** [onMove] fires as the pointer crosses each row's midpoint, not
 * on release, so the list reorders under the finger and there is one source of truth for what
 * the order is; a caller that does not move its own list sees nothing happen.
 */
@Composable
fun rememberReorderState(
    listState: LazyListState = rememberLazyListState(),
    onMove: (from: Int, to: Int) -> Unit,
): ReorderState {
    // The lambda is re-read on every drag rather than captured at the first composition, so a
    // state a caller closes over is the current one.
    val current by rememberUpdatedState(onMove)
    return remember(listState) { ReorderState(listState) { from, to -> current(from, to) } }
}

/**
 * What a reorder is doing right now: which row is lifted, and how far from home it is drawn.
 *
 * Created by [rememberReorderState]. The list is given [listState] so the gesture can read the
 * rows' bounds; a list that remembers its own scroll state separately would be reading someone
 * else's geometry.
 */
@Stable
class ReorderState internal constructor(
    val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
) {

    /** The key of the row under the finger, or `null` when nothing is being dragged. */
    var draggingKey: Any? by mutableStateOf(null)
        private set

    private var dragOffset by mutableFloatStateOf(0f)

    /** How far [key] is drawn from where the list placed it. Zero for every row but the dragged one. */
    fun offsetFor(key: Any): Float = if (key == draggingKey) dragOffset else 0f

    internal fun start(key: Any) {
        draggingKey = key
        dragOffset = 0f
    }

    internal fun drag(delta: Float) {
        val key = draggingKey ?: return
        dragOffset += delta

        val items = listState.layoutInfo.visibleItemsInfo
        val dragged = items.firstOrNull { it.key == key } ?: return

        // Where the row is drawn, not where the list placed it: the offset is everything the
        // finger has added since the last move.
        val centre = dragged.offset + dragOffset + dragged.size / 2f
        // The row the centre is over. Past the last one it is the last one, and before the first
        // it is the first — a drag off the end of the list puts the row at the end, not outside
        // it, and `visibleItemsInfo` is the right bound because a row cannot be dropped where the
        // list is not showing.
        val target = items.lastOrNull { centre >= it.offset } ?: items.first()
        if (target.index == dragged.index) return

        // One move per row crossed, not one jump: a slow drag and a flick then report the same
        // sequence, and the caller's list is consistent after each. Each step takes the crossed
        // row's own height out of the offset, so the dragged row stays under the finger rather
        // than leaping the distance a second time.
        val step = if (target.index > dragged.index) 1 else -1
        var index = dragged.index
        while (index != target.index) {
            val crossed = items.firstOrNull { it.index == index + step } ?: break
            onMove(index, crossed.index)
            dragOffset -= step * crossed.size.toFloat()
            index = crossed.index
        }
    }

    internal fun end() {
        draggingKey = null
        dragOffset = 0f
    }

    /** Whether [key] is the row being dragged — for a caller that wants to mark it. */
    fun isDragging(key: Any): Boolean = key == draggingKey
}

/**
 * The whole of what a reorderable row needs, as its `modifier`: the lift while it is the one
 * under the finger, and [appAnimateItem] — the theme's own durations — while it is not.
 *
 * The two are exclusive on purpose. A dragged row is placed by the finger and a settling row by
 * the list, and a row doing both at once travels the distance twice. [key] is the key the
 * `items {}` call uses, so the gesture and the list agree on which row this is.
 *
 * It replaces `appAnimateItem()` rather than joining it — a row that has this needs nothing else.
 */
@Composable
fun LazyItemScope.appReorderItem(state: ReorderState, key: Any): Modifier {
    val dragging = state.isDragging(key)
    return Modifier
        // Above its neighbours while it is up: a row the list has not yet re-placed would
        // otherwise be drawn under the one it is passing, which reads as the wrong row moving.
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer { translationY = state.offsetFor(key) }
        .then(if (dragging) Modifier else appAnimateItem())
}

/**
 * The gesture itself: a long press on this element lifts the row with [key], a drag moves it, and
 * a release puts it down. A tap does nothing — the handle is not a button, and the long press is
 * what separates a drag from a scroll.
 *
 * The events are taken on the **initial** pass and consumed, so the row underneath never sees
 * them. That matters because a reorderable row usually also has an `onLongClick` that starts
 * selection: without this, one press on the handle would lift the row *and* select it, and the
 * toolbar would open in the middle of a drag. It is also why the press, the timeout and the drag
 * are read from raw events rather than through `detectDragGesturesAfterLongPress`, which reads
 * the main pass and would see nothing left to work with.
 *
 * `AppReorder` is this modifier on the handle icon, and is what a list should reach for; the
 * modifier is public for the rare row whose handle is not an icon.
 */
@Composable
fun Modifier.appReorderHandle(state: ReorderState, key: Any): Modifier {
    val haptics = LocalHapticFeedback.current
    return pointerInput(state, key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            down.consume()

            // Held past the timeout, or it was a tap and the handle does nothing.
            val heldDown = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                awaitRelease(down.id)
            }
            if (heldDown != null) return@awaitEachGesture

            // The lift is felt, the way a row entering selection mode is: a long press
            // that changes what the next move means says so.
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            state.start(key)
            try {
                var last = down.position.y
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (!change.pressed) break
                    state.drag(change.position.y - last)
                    last = change.position.y
                }
            } finally {
                state.end()
            }
        }
    }
}

/** Waits for [id] to come up, consuming what it sees; never returns while the pointer is down. */
private suspend fun AwaitPointerEventScope.awaitRelease(id: PointerId) {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == id } ?: return
        change.consume()
        if (!change.pressed) return
    }
}
