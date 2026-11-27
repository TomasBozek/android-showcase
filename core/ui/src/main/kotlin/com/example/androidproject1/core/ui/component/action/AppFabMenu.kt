package com.example.androidproject1.core.ui.component.action

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.height
import com.example.androidproject1.core.ui.layout.appCoachTarget
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.keySurface

/**
 * One choice of an [AppFabMenu].
 *
 * @param testTag what a flow and a test find the action by — `<stem>_<name>Item`. An action is a
 * plain value, so there is no modifier to hang one on; the same treatment as `MenuItem`'s.
 */
data class FabAction(
    val label: String,
    val icon: ImageVector,
    val testTag: String? = null,
    val onClick: () -> Unit,
)

/**
 * A speed dial: one key that opens into a few — the things a screen creates when there is
 * more than one, shown where the thumb already is rather than in a sheet you open for one choice.
 *
 * The key turns 45° on `pressMillis` so the plus reads as a cross while it is open; the actions
 * stagger up as labelled mini keys — `ControlSize.Small`, `toggleMillis` each, 40 ms apart — and a
 * scrim at `scrimAlpha` sits under them, so a tap anywhere else collapses the dial. An action
 * reports and collapses. Everything travels on `keySurface`, like [AppFab], never on a shadow.
 *
 * The caller owns `expanded`, the way it owns a sheet's, so a test can read it and a rotation
 * keeps it. Place it where a FAB goes: bottom-end of the screen's `Box`, over the content.
 *
 * @param keyTestTag the key's own id — `<stem>_<name>Button`. The [modifier] tags the whole dial,
 * scrim included, and a flow that tapped its centre tapped the middle of the screen (H0 ship).
 */
@Composable
fun AppFabMenu(
    icon: ImageVector,
    contentDescription: String,
    items: List<FabAction>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    keyTestTag: String? = null,
) {
    val family = AppTheme.colors.confirm
    val colors = AppTheme.colors
    val motion = AppTheme.motion
    val rotation by animateFloatAsState(
        targetValue = if (expanded) OPEN_ROTATION else 0f,
        animationSpec = tween(durationMillis = motion.pressMillis, easing = motion.pressEasing),
        label = "fabMenuRotation",
    )

    Box(modifier = modifier) {
        // The scrim spans whatever the caller gave the dial: bottom-end of the screen's Box means
        // the whole screen, which is what makes "tap anywhere else" work.
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(motion.toggleMillis, easing = motion.toggleEasing)),
            exit = fadeOut(tween(motion.toggleMillis, easing = motion.toggleEasing)),
            modifier = Modifier.matchParentSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.scrim.copy(alpha = colors.scrimAlpha))
                    .testTag(FAB_MENU_SCRIM_TAG)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onExpandedChange(false) },
                    ),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppTheme.spacing.inset.lg),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
        ) {
            // The last action sits nearest the key and arrives first: the dial grows upward.
            items.forEachIndexed { index, action ->
                val order = items.size - 1 - index
                val delay = order * STAGGER_MILLIS
                AnimatedVisibility(
                    visible = expanded,
                    enter = scaleIn(tween(motion.toggleMillis, delay, motion.toggleEasing)) +
                        fadeIn(tween(motion.toggleMillis, delay, motion.toggleEasing)),
                    exit = scaleOut(tween(motion.toggleMillis, easing = motion.toggleEasing)) +
                        fadeOut(tween(motion.toggleMillis, easing = motion.toggleEasing)),
                ) {
                    FabMenuAction(
                        action = action,
                        onClick = {
                            action.onClick()
                            onExpandedChange(false)
                        },
                    )
                }
            }
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .size(KEY_SIZE)
                    .then(
                        if (keyTestTag != null) {
                            Modifier.testTag(keyTestTag).appCoachTarget(keyTestTag)
                        } else {
                            Modifier
                        },
                    )
                    .semantics { stateDescription = if (expanded) "expanded" else "collapsed" }
                    .keySurface(
                        color = family.bg,
                        edge = family.edge,
                        shape = AppTheme.shapes.pill,
                        pressed = pressed,
                    )
                    .clickable(
                        role = Role.Button,
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onExpandedChange(!expanded) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = family.label,
                    modifier = Modifier
                        .size(AppTheme.icons.md)
                        .rotate(rotation),
                )
            }
        }
    }
}

/** A labelled mini key: the label on a raised surface beside the key, both pressable. */
@Composable
private fun FabMenuAction(
    action: FabAction,
    onClick: () -> Unit,
) {
    val family = AppTheme.colors.confirm
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .then(if (action.testTag != null) Modifier.testTag(action.testTag) else Modifier)
            .clickable(
                role = Role.Button,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
    ) {
        Box(
            modifier = Modifier
                .keySurface(
                    color = colors.surfaceRaised,
                    edge = colors.borderStrong,
                    shape = AppTheme.shapes.md,
                    pressed = pressed,
                )
                .padding(horizontal = AppTheme.spacing.inset.md, vertical = AppTheme.spacing.inset.sm),
        ) {
            AppText(text = action.label, role = TextRole.Label, color = colors.textPrimary)
        }
        Box(
            modifier = Modifier
                .size(ControlSize.Small.height)
                .keySurface(
                    color = family.bg,
                    edge = family.edge,
                    shape = AppTheme.shapes.pill,
                    pressed = pressed,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = family.label,
                modifier = Modifier.size(AppTheme.icons.sm),
            )
        }
    }
}

/** The scrim under an open dial — the dial's own element, so its id is a constant, like the alert's. */
const val FAB_MENU_SCRIM_TAG = "fabMenu_scrimTile"

/** A quarter turn less a bit: a plus at 45° is a cross, and a cross says "close". */
private const val OPEN_ROTATION = 45f

/** Between one action arriving and the next — enough to read as a stagger, not as a wait. */
private const val STAGGER_MILLIS = 40

private val KEY_SIZE = 56.dp

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    Box(modifier = Modifier.height(PREVIEW_HEIGHT)) {
        AppFabMenu(
            icon = Icons.Filled.Add,
            contentDescription = "Create",
            items = listOf(
                FabAction("New trip", Icons.Filled.DateRange) {},
                FabAction("Add a place", Icons.Filled.Place) {},
            ),
            expanded = true,
            onExpandedChange = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val PREVIEW_HEIGHT = 240.dp
