package com.example.androidproject1.core.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Layer 2 — motion. Nine tokens, and every animation in the system reads one of them; a literal
 * duration anywhere under `core/ui` or `app` is a bug.
 *
 * No animation blocks the next input: a second touch during a running animation still counts.
 * Reduced motion is a theme input: `AppTheme(reducedMotion = …)` defaults to
 * [systemReducedMotion], and when it is true the theme provides [reduced] — every duration zero,
 * the easings unchanged — so every reader lands on its end state on the first frame without a
 * branch of its own. The press still registers: the key's edge shortens and the colour changes at
 * once rather than over 70 ms. A loop reads [ambientMillis] before it starts, and at zero draws
 * its rest frame instead.
 */
@Immutable
data class AppMotion(
    /** Key travel and press feedback: `keySurface`, `AppFabMenu`'s turning key. */
    val pressMillis: Int = 70,
    val pressEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    /**
     * A state that flips: `AppSwitch`, `AppCheckbox`, `AppPager`'s dot, `AppProgress`'s fill,
     * `AppAccordion`'s expand, `AppFabMenu`'s actions.
     */
    val toggleMillis: Int = 140,
    val toggleEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f),
    /** Sheet and dialog: decelerate in, accelerate out. */
    val sheetEnterMillis: Int = 220,
    val sheetExitMillis: Int = 180,
    /**
     * Something that appears rather than moves — `AppScrollShadow`, a banner, a badge, the
     * loading scrim — and the tab-root cross-fade in `AppNavHost`.
     */
    val fadeMillis: Int = 160,
    /**
     * One cycle of a loop that never ends: `AppSkeleton`'s pulse, `AppIndeterminateProgress`'s
     * sweep. Zero means the loop does not run and the reader draws its rest frame.
     */
    val ambientMillis: Int = 1_000,
    /**
     * Screen transitions — the push and the pop in `AppNavHost`, and the bounds of a shared
     * element (`Modifier.appSharedElement`), so the two end together.
     */
    val screenMillis: Int = 260,
) {

    /** The same tokens with every duration at zero: what the theme provides under reduced motion. */
    fun reduced(): AppMotion = copy(
        pressMillis = 0,
        toggleMillis = 0,
        sheetEnterMillis = 0,
        sheetExitMillis = 0,
        fadeMillis = 0,
        ambientMillis = 0,
        screenMillis = 0,
    )
}

/**
 * Whether the device asks for no animation: Android's "Remove animations" accessibility setting
 * writes a zero `ANIMATOR_DURATION_SCALE`, and so does a developer option. The default is `1f`,
 * motion on — Robolectric answers null for a setting nobody wrote, and null must mean motion on.
 * Re-read when the configuration changes, the same way [pointerPresent] is.
 */
@Composable
fun systemReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    val configuration = LocalConfiguration.current
    return remember(resolver, configuration) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

internal val LocalAppMotion = staticCompositionLocalOf { AppMotion() }
