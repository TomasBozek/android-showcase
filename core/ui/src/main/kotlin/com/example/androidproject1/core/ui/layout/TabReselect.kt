package com.example.androidproject1.core.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A second tap on the tab already on screen. The host pops the tab's segment to its root
 * and emits once here; a tab root collects it and scrolls its list to the top — what every bar
 * does. Empty everywhere the host is not around it:
 * a preview, a test and the auth flow see a flow that never emits.
 *
 * `Unit`, not the tab: only the tab on screen is composed, so the one root collecting is the one
 * the tap was on.
 */
val LocalTabReselect = staticCompositionLocalOf<Flow<Unit>> { emptyFlow() }

/** Runs [onReselect] each time the tab this root sits on is tapped again — one line in a root. */
@Composable
fun TabReselectEffect(onReselect: suspend () -> Unit) {
    val signal = LocalTabReselect.current
    LaunchedEffect(signal) { signal.collect { onReselect() } }
}
