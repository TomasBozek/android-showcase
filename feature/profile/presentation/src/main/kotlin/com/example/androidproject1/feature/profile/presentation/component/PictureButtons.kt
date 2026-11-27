package com.example.androidproject1.feature.profile.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.profile.presentation.R
import com.example.androidproject1.feature.profile.presentation.profile.ProfileEvent

/**
 * The two ways to get a picture, in the order they cost the user something.
 *
 * The Photo Picker asks for no permission at all — the system shows the gallery, the user chooses
 * one picture, and this app is handed that one and nothing else. The camera needs a permission,
 * so it is the second button and it opens the gated section below rather than the camera itself.
 */
@Composable
fun PictureButtons(onEvent: (ProfileEvent) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
    ) {
        AppButton(
            label = stringResource(R.string.profile_choose_photo),
            onClick = { onEvent(ProfileEvent.ChoosePhotoClicked) },
            kind = ButtonKind.Outline,
            modifier = Modifier
                .weight(1f)
                .testTag("profile_choosePhotoButton"),
        )

        AppButton(
            label = stringResource(R.string.profile_take_photo),
            onClick = { onEvent(ProfileEvent.TakePhotoClicked) },
            kind = ButtonKind.Outline,
            modifier = Modifier
                .weight(1f)
                .testTag("profile_takePhotoButton"),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    PictureButtons(onEvent = {})
}
