package com.example.androidproject1.service.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import kotlin.coroutines.cancellation.CancellationException

/**
 * Intercepts the back gesture while [dirty], and runs [onBack] when it completes.
 *
 * `PredictiveBackHandler` rather than `BackHandler` so the gesture still animates: the swipe is
 * followed, and only a *finished* one asks the question. A cancelled swipe — the one where a thumb
 * changes its mind halfway — throws `CancellationException` out of the collect and must ask
 * nothing, which is the whole reason this is not a one-line `BackHandler`.
 *
 * **It stands aside while the keyboard is up.** The first back on a form being typed into closes
 * the keyboard, and nothing is lost by that; an enabled handler was handed the same back as well,
 * so a sign-in form asked "Discard your changes?" the moment its keyboard went away.
 *
 * The question itself is the `discardAlert()` the view model raises (`:service:core:ui`), so it is
 * drawn by `Screen()` like every other alert and the screen adds no dialog of its own.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun DiscardBackHandler(dirty: Boolean, onBack: () -> Unit) {
    val keyboardUp = WindowInsets.isImeVisible
    PredictiveBackHandler(enabled = dirty && !keyboardUp) { progress ->
        try {
            progress.collect { }
            onBack()
        } catch (_: CancellationException) {
            // The gesture was abandoned. Nothing was going to be lost, so nothing is asked.
        }
    }
}
