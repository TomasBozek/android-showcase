package com.example.androidproject1.core.ui.component.list

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.choice.AppCheckbox
import com.example.androidproject1.core.ui.component.content.AppIcon
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.status.AppAvatar
import com.example.androidproject1.core.ui.component.status.AppTag
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.core.ui.component.status.feedback
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.pressFeedback
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One action a swipe on a row reveals: a key of [tone]'s colours, [minTouchTarget] wide,
 * showing [icon] with [label] as its description. [testTag] is what a test and a flow tap once it
 * is out — the key is a button, which is the whole point of revealing rather than dismissing.
 */
@Immutable
data class SwipeAction(
    val label: String,
    val icon: ImageVector,
    val onAction: () -> Unit,
    val tone: TagTone = TagTone.Neutral,
    val testTag: String? = null,
)

/**
 * What a row reveals: [end] on a swipe towards the start edge (the common one — delete, remove),
 * and optionally [start] on a swipe the other way. [initiallyRevealed] starts the row with the
 * end key out — for a preview, a gallery variant or a golden, none of which can swipe.
 */
@Immutable
data class SwipeActions(
    val end: SwipeAction,
    val start: SwipeAction? = null,
    val initiallyRevealed: Boolean = false,
)

/** Where a swiped row rests. */
private enum class SwipePosition { Start, Closed, End }

/**
 * The base of every list in the app.
 *
 * Its height is the density's row height — 56 dp on touch — so a row is a comfortable target
 * without anything inside it having to grow. [leading] is where an avatar or an icon goes, and
 * [trailing] a tag or a value.
 *
 * [selected] is for a row that is currently *shown elsewhere* — the chosen item of a list–detail
 * pair on a wide screen. It is not the same as checked, which is [AppCheckbox]'s job and is state
 * the user set; a selected row is where the app is, and it says so to a screen reader through
 * `Role.Tab`'s selected state rather than by colour alone.
 *
 * [onLongClick] is how a list enters selection mode. It needs [onClick] too: a row that can only
 * be long-pressed is a row a screen reader cannot reach, so the long press is an addition to a
 * tap, never a replacement.
 *
 * [headlineModifier] reaches the headline's own text — for `Modifier.appSharedElement`, so the
 * name can travel to the screen the row opens. The row does not know why.
 *
 * [destructive] draws the headline in the destructive role — a row that logs out or deletes,
 * in a settings list's danger zone. What guards the action is the confirm dialog behind it, not
 * the colour; the colour is what tells the row apart before the tap.
 *
 * [dragHandle] draws a handle at the row's end — `AppReorder`, which a long press lifts the row
 * by. It is the only thing on the row that drags vertically: the row itself must stay
 * scrollable, and it already drags sideways when it has [swipeActions].
 *
 * [swipeActions] puts a key behind the row: a swipe drags the row aside over
 * `anchoredDraggable` and reveals it, a tap on the key runs the action, and the row settles back
 * over `toggleMillis`. Never a swipe-through — the key is a button a screen reader, a test and a
 * flow can press — and the same action stays reachable some other way, since a swipe is a
 * gesture nobody is told about. Reduced motion settles the row on the first frame.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    destructive: Boolean = false,
    headlineModifier: Modifier = Modifier,
    swipeActions: SwipeActions? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    dragHandle: @Composable (() -> Unit)? = null,
) {
    if (swipeActions == null) {
        ListItemRow(
            headline = headline,
            modifier = modifier,
            supporting = supporting,
            onClick = onClick,
            onLongClick = onLongClick,
            selected = selected,
            destructive = destructive,
            headlineModifier = headlineModifier,
            leading = leading,
            trailing = trailing,
            dragHandle = dragHandle,
        )
        return
    }
    SwipeableRow(actions = swipeActions, modifier = modifier) { rowModifier ->
        ListItemRow(
            headline = headline,
            modifier = rowModifier,
            supporting = supporting,
            onClick = onClick,
            onLongClick = onLongClick,
            selected = selected,
            destructive = destructive,
            headlineModifier = headlineModifier,
            leading = leading,
            trailing = trailing,
            dragHandle = dragHandle,
        )
    }
}

/**
 * The keys behind a row and the drag that reveals them. The row is drawn by [content] with the
 * offset modifier, over a box that holds the keys at its two ends; the anchors are the key's
 * width, so a settled row shows exactly one key and no more.
 */
@Composable
private fun SwipeableRow(
    actions: SwipeActions,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val motion = AppTheme.motion
    val keyWidth = AppTheme.density.minTouchTarget
    val keyWidthPx = with(density) { keyWidth.toPx() }
    val state = remember(keyWidthPx, actions.start != null) {
        AnchoredDraggableState(
            initialValue = if (actions.initiallyRevealed) SwipePosition.End else SwipePosition.Closed,
            anchors = DraggableAnchors {
                SwipePosition.Closed at 0f
                SwipePosition.End at -keyWidthPx
                if (actions.start != null) SwipePosition.Start at keyWidthPx
            },
        )
    }
    val snap = tween<Float>(motion.toggleMillis, easing = motion.toggleEasing)
    val scope = rememberCoroutineScope()
    fun settle(action: SwipeAction) {
        action.onAction()
        scope.launch { state.animateTo(SwipePosition.Closed, snap) }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        // The key sits under the row and takes its height, and it is exactly as wide as the strip
        // the row has vacated: the row keeps its transparent surface, so a key the row's width
        // would show through it mid-drag, and one the strip's width cannot.
        val revealed = with(density) { abs(state.offset).toDp() }
        actions.start?.let { start ->
            if (state.offset > 0f) {
                SwipeKey(
                    action = start,
                    onClick = { settle(start) },
                    modifier = Modifier.matchParentSize(),
                    align = Alignment.CenterStart,
                    width = revealed,
                )
            }
        }
        if (state.offset < 0f) {
            SwipeKey(
                action = actions.end,
                onClick = { settle(actions.end) },
                modifier = Modifier.matchParentSize(),
                align = Alignment.CenterEnd,
                width = revealed,
            )
        }
        content(
            Modifier
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                        state = state,
                        animationSpec = snap,
                    ),
                ),
        )
    }
}

@Composable
private fun SwipeKey(
    action: SwipeAction,
    onClick: () -> Unit,
    modifier: Modifier,
    align: Alignment,
    width: Dp,
) {
    val feedback = action.tone.feedback(AppTheme.colors)
    Box(modifier = modifier, contentAlignment = align) {
        // The strip grows with the drag; the icon is centred in the key's full width and clipped
        // until the strip is wide enough to show it whole.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(width)
                .clipToBounds()
                .background(feedback.container)
                .clickable(role = Role.Button, onClick = onClick)
                .then(if (action.testTag != null) Modifier.testTag(action.testTag) else Modifier),
            contentAlignment = align,
        ) {
            Box(
                modifier = Modifier.width(AppTheme.density.minTouchTarget),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(icon = action.icon, contentDescription = action.label, tint = feedback.onContainer)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListItemRow(
    headline: String,
    modifier: Modifier,
    supporting: String?,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    selected: Boolean,
    destructive: Boolean,
    headlineModifier: Modifier,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    dragHandle: @Composable (() -> Unit)?,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            // A row is a pressable with no edge, so it scales under the finger; a row that
            // does nothing on a tap does not.
            .then(if (onClick != null) Modifier.pressFeedback(interaction) else Modifier)
            .then(
                // A row that can be the shown one reports *which* it is, so a screen reader says
                // "selected" rather than only "button"; a row that cannot is a plain button.
                when {
                    onClick == null -> Modifier
                    selected -> Modifier.selectable(
                        selected = true,
                        role = Role.Tab,
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                    onLongClick != null -> Modifier.combinedClickable(
                        role = Role.Button,
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = onClick,
                        // A long press is a state change — selection mode begins — and is felt.
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        },
                    )
                    else -> Modifier.clickable(
                        role = Role.Button,
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                },
            )
            .background(if (selected) AppTheme.colors.selection.container else Color.Transparent)
            .defaultMinSize(minHeight = AppTheme.density.listRowHeight)
            .padding(
                horizontal = AppTheme.spacing.inset.lg,
                vertical = AppTheme.spacing.inset.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
        ) {
            AppText(
                text = headline,
                role = TextRole.Body,
                color = if (destructive) AppTheme.colors.destructive.bg else null,
                modifier = headlineModifier,
            )
            if (supporting != null) {
                AppText(text = supporting, role = TextRole.Secondary)
            }
        }
        trailing?.invoke()
        // After the trailing content, at the edge the thumb reaches: a handle is grabbed, not
        // read, so it sits where the hand already is rather than where the eye starts.
        dragHandle?.invoke()
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppListItem(headline = "Pilsner Urquell", supporting = "0,5 l", onClick = {})
    AppListItem(
        headline = "Kofola",
        supporting = "0,5 l — shown on the right",
        onClick = {},
        selected = true,
    )
    AppListItem(
        headline = "Camera",
        supporting = "android.permission.CAMERA",
        trailing = { AppTag(label = "Granted", tone = TagTone.Positive) },
    )
    AppListItem(
        headline = "Cordless drill",
        supporting = "Jana Nováková",
        onClick = {},
        leading = { AppAvatar(name = "Jana Nováková") },
        trailing = { AppTag(label = "Good") },
    )
    AppListItem(headline = "Log out", onClick = {}, destructive = true)
    AppListItem(
        headline = "Kofola",
        supporting = "swiped: the key is out",
        onClick = {},
        swipeActions = SwipeActions(
            end = SwipeAction("Remove", Icons.Filled.Delete, onAction = {}, tone = TagTone.Negative),
            initiallyRevealed = true,
        ),
    )
}
