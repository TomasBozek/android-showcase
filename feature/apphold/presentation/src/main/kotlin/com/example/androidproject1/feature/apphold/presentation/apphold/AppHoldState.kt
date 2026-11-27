package com.example.androidproject1.feature.apphold.presentation.apphold

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.androidproject1.feature.apphold.api.AppHoldReason

@Immutable
data class AppHoldState(
    val reason: AppHoldReason,
) {

    companion object {

        val PREVIEW = AppHoldState(reason = AppHoldReason.UpdateRequired)
    }
}

/** The two states this screen is drawn in: an update required, and maintenance. */
class AppHoldStatePreviews : PreviewParameterProvider<AppHoldState> {

    override val values = sequenceOf(
        AppHoldState.PREVIEW,
        AppHoldState(reason = AppHoldReason.Maintenance),
    )
}
