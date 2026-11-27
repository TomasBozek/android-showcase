package com.example.androidproject1.feature.profile.presentation.avatarcrop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppImageCropper
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.profile.presentation.R

@Composable
fun AvatarCropScreen(
    state: AvatarCropState,
    onEvent: (AvatarCropEvent) -> Unit,
) {
    AppScaffold(
        screenId = "AvatarCropScreen",
        topBar = {
            // Up is Cancel: the avatar stays as it was.
            AppTopBar(
                title = stringResource(R.string.avatar_crop_title),
                onNavigateUp = { onEvent(AvatarCropEvent.CancelClicked) },
                navigateUpTestTag = "avatarCrop_upButton",
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.spacing.inset.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AppImageCropper(
                    model = state.uri,
                    onCropChange = { onEvent(AvatarCropEvent.CropChanged(it)) },
                    modifier = Modifier.testTag("avatarCrop_imageGroup"),
                )
            }
            AppText(text = stringResource(R.string.avatar_crop_hint), role = TextRole.Secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md)) {
                AppButton(
                    label = stringResource(R.string.avatar_crop_cancel),
                    onClick = { onEvent(AvatarCropEvent.CancelClicked) },
                    kind = ButtonKind.Outline,
                    modifier = Modifier.weight(1f).testTag("avatarCrop_cancelButton"),
                )
                AppButton(
                    label = stringResource(R.string.avatar_crop_done),
                    onClick = { onEvent(AvatarCropEvent.DoneClicked) },
                    enabled = state.canFinish,
                    modifier = Modifier.weight(1f).testTag("avatarCrop_doneButton"),
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    AvatarCropScreen(state = AvatarCropState.PREVIEW) {}
}
