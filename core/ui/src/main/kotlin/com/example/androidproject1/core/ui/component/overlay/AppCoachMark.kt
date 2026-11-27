package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppCard
import com.example.androidproject1.core.ui.component.status.AppStepProgress
import com.example.androidproject1.core.ui.layout.LocalCoachTargets
import com.example.androidproject1.core.ui.layout.appCoachTarget
import com.example.androidproject1.core.ui.layout.rememberCoachTargets
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * One stop on a tour: which element it points at, and what it says about it.
 *
 * [targetId] is the element's own test tag — the id it registered with `Modifier.appCoachTarget`
 * — so a step names the same string the test and the screen reader do.
 */
@Immutable
data class CoachStep(
    val targetId: String,
    val title: String,
    val body: String,
)

/**
 * The tour's one step on screen: everything dimmed but the thing being talked about, and a card
 * saying what it is.
 *
 * The scrim is `AppTheme.colors.scrim` with a rounded hole punched over the target's bounds, read
 * from `LocalCoachTargets` — so the hole is over the real element wherever the layout put it, and
 * the screen underneath needs no code beyond the modifier that registered it.
 *
 * **A step whose target is not registered draws nothing and calls [onNext].** A hole at the origin
 * with a card beside it is worse than no step, and a tour that stops dead on a screen where an
 * element is conditional is worse still.
 *
 * **The scrim is not a dismiss.** A tap on it does nothing: a tour is left by Skip, which says so,
 * and a stray tap on a dimmed screen is the commonest way to lose one by accident. Focus moves to
 * the card as each step appears and the title is an assertive live region, so a screen reader
 * announces the new step rather than leaving the user on whatever they last touched.
 */
@Composable
fun AppCoachMark(
    step: CoachStep,
    stepIndex: Int,
    stepCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targets = LocalCoachTargets.current
    val target = targets?.get(step.targetId)
    if (target == null) {
        // Not missing yet: an element registers its bounds during *layout*, which happens after
        // the composition that put this mark on screen, so every target is legitimately absent
        // on the first frame. Waiting one frame is the difference between a tour that works and
        // one that skips its own first step. Keyed on the id, so a tour that lands on three
        // missing targets in a row walks through all three.
        LaunchedEffect(step.targetId, targets) {
            withFrameNanos {}
            if (targets?.get(step.targetId) == null) onNext()
        }
        return
    }

    val colors = AppTheme.colors
    val motion = AppTheme.motion
    val shape = AppTheme.shapes.md
    val density = LocalDensity.current
    val breathingRoom = with(density) { AppTheme.spacing.inset.sm.toPx() }
    val gap = with(density) { AppTheme.spacing.stack.md.toPx() }

    // The hole travels between steps on the token the screen transitions use, so a tour reads as
    // one thing moving rather than a series of jumps.
    val hole by animateRectAsState(
        targetValue = Rect(
            left = target.left - breathingRoom,
            top = target.top - breathingRoom,
            right = target.right + breathingRoom,
            bottom = target.bottom + breathingRoom,
        ),
        animationSpec = tween(motion.screenMillis),
        label = "coachHole",
    )
    val focus = remember { FocusRequester() }
    LaunchedEffect(step.targetId) { focus.requestFocus() }

    // Where the overlay itself sits in the root. A target's bounds are the root's, and the scrim
    // draws in its own, so the two only agree when the overlay happens to start at the origin —
    // which it does full-screen and does not inside a card, a gallery page or under a top bar.
    var origin by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.boundsInRoot().topLeft },
    ) {
        val containerHeight = constraints.maxHeight
        val local = hole.translate(-origin.x, -origin.y)
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Swallowed, not handled: the scrim stops a tap reaching what is under it, and
                // does nothing itself.
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                // An offscreen layer, or `BlendMode.Clear` would clear the window rather than the
                // scrim drawn into it.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(color = colors.scrim, alpha = colors.scrimAlpha)
                    val radius = shape.topStart.toPx(Size(local.width, local.height), this)
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(local.left, local.top),
                        size = Size(local.width, local.height),
                        cornerRadius = CornerRadius(radius, radius),
                        blendMode = BlendMode.Clear,
                    )
                },
        )

        var cardHeight by remember { mutableIntStateOf(0) }
        // The gaps the card has to live in. Below the hole by preference — that is where the eye
        // goes next — and above it when the card does not fit below.
        val roomBelow = containerHeight - (local.bottom + gap)
        val roomAbove = local.top - gap
        // Until the card has been measured it has no height, and a card of no height "fits"
        // anywhere — so it would go below, be capped to the room there, measure no taller than
        // that room, and so keep fitting: off-screen for good under a tab at the bottom.
        // The first frame goes to the side with more room; after that the card's own height decides.
        val below = if (cardHeight == 0) roomBelow >= roomAbove else cardHeight <= roomBelow || roomBelow >= roomAbove
        val room = (if (below) roomBelow else roomAbove).coerceAtLeast(0f)
        val maxWidth = AppTheme.density.contentMaxWidth
        Box(
            modifier = Modifier
                // The overlay is composed over the nav host, outside any `AppScaffold`, so the
                // reading width that caps every screen has to be applied here. The cap
                // goes first, as in the scaffold, and the card is centred inside the window.
                .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(AppTheme.spacing.inset.lg)
                .offset {
                    // Never over the hole: a card covering the element it points at is the one
                    // thing the placement must not do, so on a screen too short for both the
                    // card gives up its own height rather than the target's visibility.
                    val y = if (below) local.bottom + gap else (local.top - gap - cardHeight).coerceAtLeast(0f)
                    IntOffset(0, y.toInt().coerceAtLeast(0))
                }
                .onGloballyPositioned { cardHeight = it.size.height },
        ) {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = with(density) { room.toDp() })
                    .focusRequester(focus)
                    .focusable()
                    .testTag("tour_dialog"),
            ) {
                // What it says scrolls; the two buttons do not. On a screen with room for
                // neither the card nor the target, the way out of a tour is the last thing that
                // should be below the fold.
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
                ) {
                    AppText(
                        text = step.title,
                        role = TextRole.Title,
                        // Assertive, so the new step is read as it arrives rather than after
                        // whatever the user last touched.
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                    AppText(text = step.body, role = TextRole.Body)
                    AppStepProgress(
                        steps = stepCount,
                        currentStep = stepIndex,
                        modifier = Modifier.testTag("tour_progress"),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Skip first in the order a screen reader walks: leaving is what someone
                    // reaches for when a tour has appeared over what they meant to do.
                    AppButton(
                        label = stringResource(R.string.app_coach_skip),
                        onClick = onSkip,
                        kind = ButtonKind.Ghost,
                        modifier = Modifier.testTag("tour_skipButton"),
                    )
                    AppButton(
                        label = stringResource(
                            if (stepIndex == stepCount - 1) R.string.app_coach_done else R.string.app_coach_next,
                        ),
                        onClick = onNext,
                        kind = ButtonKind.Confirm,
                        modifier = Modifier.testTag("tour_nextButton"),
                    )
                }
            }
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    val targets = rememberCoachTargets()
    CompositionLocalProvider(LocalCoachTargets provides targets) {
        Box(modifier = Modifier.fillMaxWidth().height(AppTheme.density.listRowHeight * PREVIEW_ROWS)) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                AppButton(label = "Weather", onClick = {}, modifier = Modifier.appCoachTarget(PREVIEW_TARGET))
                AppButton(label = "Settings", onClick = {})
            }
            AppCoachMark(
                step = CoachStep(PREVIEW_TARGET, "Your places", "Everywhere you keep an eye on."),
                stepIndex = 0,
                stepCount = PREVIEW_STEPS,
                onNext = {},
                onSkip = {},
            )
        }
    }
}

private const val PREVIEW_TARGET = "preview_coachTarget"
private const val PREVIEW_STEPS = 3

/** Tall enough for the two targets and the card under them. */
private const val PREVIEW_ROWS = 6
