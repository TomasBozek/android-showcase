package com.example.androidproject1.feature.auth.presentation.applock

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/** @property asking whether the system's prompt is up right now; the button waits for it. */
@Immutable
data class AppLockState(
    val asking: Boolean = false,
) {

    companion object {

        val PREVIEW = AppLockState()
    }
}

/** The two states this screen is drawn in: waiting for a tap, and the prompt up over it. */
class AppLockStatePreviews : PreviewParameterProvider<AppLockState> {

    override val values = sequenceOf(
        AppLockState.PREVIEW,
        AppLockState.PREVIEW.copy(asking = true),
    )
}
