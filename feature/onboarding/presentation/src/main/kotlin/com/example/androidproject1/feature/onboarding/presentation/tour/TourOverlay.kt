package com.example.androidproject1.feature.onboarding.presentation.tour

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.androidproject1.core.ui.component.overlay.AppCoachMark
import com.example.androidproject1.core.ui.component.overlay.CoachStep

/**
 * The tour, drawn over whatever screen the user is on.
 *
 * Nothing when the tour is not running, which is most of the time — so the host composes it
 * unconditionally and this decides. `AppCoachMark` finds the step's target in
 * `LocalCoachTargets` and does the rest; a step whose target is not on the screen the user is
 * looking at advances by itself, which is what lets one list of steps span a tab root, its bar
 * and its menu icon without the tour knowing which of them is on screen.
 */
@Composable
fun TourOverlay(
    state: TourState,
    onEvent: (TourEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = state.current ?: return
    AppCoachMark(
        step = CoachStep(targetId = step.targetId, title = step.title, body = step.body),
        stepIndex = state.step ?: 0,
        stepCount = state.steps.size,
        onNext = { onEvent(TourEvent.Next) },
        onSkip = { onEvent(TourEvent.Skip) },
        modifier = modifier,
    )
}
