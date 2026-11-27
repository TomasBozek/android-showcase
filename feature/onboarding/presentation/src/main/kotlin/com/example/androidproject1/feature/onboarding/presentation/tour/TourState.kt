package com.example.androidproject1.feature.onboarding.presentation.tour

import androidx.compose.runtime.Immutable

/**
 * One stop on the tour, as the host names it.
 *
 * [targetId] is the element's own test tag — the id it registers with `Modifier.appCoachTarget` —
 * so a step names a string that already exists rather than a second copy of one. The copy is
 * resolved by the host, which owns the strings, and arrives here as text.
 */
@Immutable
data class TourStep(
    val targetId: String,
    val title: String,
    val body: String,
)

/**
 * Where the tour is: `null` when it is not running, otherwise the index into [steps].
 *
 * Not a `UiState`: the tour is not a screen. It has no loading overlay and no error dialog, and
 * it is drawn over whatever screen the user is on — `Screen()` would paint a full-size surface
 * over that screen and defeat the point.
 */
@Immutable
data class TourState(
    val step: Int? = null,
    val steps: List<TourStep> = emptyList(),
) {

    val current: TourStep? get() = step?.let(steps::getOrNull)

    val running: Boolean get() = current != null

    companion object {

        /**
         * A tour on its second of three steps, pointing at Home's first door.
         *
         * Not a screen's fixture — the tour has no screen — but the shape a preview or a test
         * starts from, and what `doctor.py` asks every `*State` for.
         */
        val PREVIEW = TourState(
            step = 1,
            steps = listOf(
                TourStep("home_catalogCard", "The shop", "Everything for sale."),
                TourStep("menu_openButton", "Everything else", "Trips, the inventory, the weather."),
                TourStep("tabs_settingsTab", "Settings", "The theme, the language, the way out."),
            ),
        )
    }
}
