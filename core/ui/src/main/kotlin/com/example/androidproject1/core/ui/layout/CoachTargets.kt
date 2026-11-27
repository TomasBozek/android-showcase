package com.example.androidproject1.core.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Where the things a tour points at actually are.
 *
 * An element marks itself with [Modifier.appCoachTarget] under the id its test tag already
 * carries, and the host reads the bounds back when it walks a tour. The element does not know a
 * tour exists, and a screen being toured needs no code of its own.
 */
@Stable
class CoachTargets {

    private val bounds = mutableStateMapOf<String, Rect>()

    /** Where [id] is on screen, or `null` when nothing registered it or it has left. */
    operator fun get(id: String): Rect? = bounds[id]

    internal fun put(id: String, rect: Rect) {
        bounds[id] = rect
    }

    internal fun remove(id: String) {
        bounds.remove(id)
    }
}

/**
 * The registry the host provides around whatever can be toured — `null` everywhere else, which is
 * what a preview, a test and the gallery see.
 *
 * `null` rather than an empty registry on purpose: [Modifier.appCoachTarget] is then a no-op that
 * adds no layout callback at all, so an element outside a host draws exactly as it would without
 * the modifier and its golden does not move.
 */
val LocalCoachTargets = staticCompositionLocalOf<CoachTargets?> { null }

/** A registry to provide, remembered for as long as the host that owns it. */
@Composable
fun rememberCoachTargets(): CoachTargets = remember { CoachTargets() }

/**
 * Marks this element as the thing a tour step with [id] points at.
 *
 * [id] is the element's own test tag — `home_weatherCard`, `tabs_settingsTab` — so a step names
 * the same string the test and the screen reader do, and there is one to keep in sync rather than
 * two. A no-op without a host: no callback, no bounds, nothing drawn differently.
 *
 * The bounds are the element's in the root, re-read on every layout, so a step still points at the
 * right place after a scroll or a rotation; when the element leaves composition it unregisters,
 * and a step whose target is gone is one the tour skips.
 */
@Composable
fun Modifier.appCoachTarget(id: String): Modifier {
    val targets = LocalCoachTargets.current ?: return this
    DisposableEffect(targets, id) {
        onDispose { targets.remove(id) }
    }
    return onGloballyPositioned { targets.put(id, it.boundsInRoot()) }
}
