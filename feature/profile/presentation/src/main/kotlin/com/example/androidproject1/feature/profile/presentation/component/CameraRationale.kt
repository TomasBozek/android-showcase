package com.example.androidproject1.feature.profile.presentation.component

import android.Manifest
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppCard
import com.example.androidproject1.feature.profile.presentation.R
import com.example.androidproject1.feature.profile.presentation.profile.ProfileEvent
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.permission.PermissionRationale
import com.example.androidproject1.service.permission.PermissionRequest
import com.example.androidproject1.service.permission.PermissionStatus
import com.example.androidproject1.service.permission.permissionRationale
import com.example.androidproject1.service.permission.rememberPermissionRequest

/** Why this app wants the camera, and the one button that can still do something about it. */
@Composable
fun CameraRationale(
    rationale: PermissionRationale,
    request: PermissionRequest,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = request.status
    val canAskAgain = status !is PermissionStatus.RefusedForGood

    AppCard(modifier = modifier.testTag("profile_cameraRationaleTile")) {
        AppText(text = rationale.title.resolve(), role = TextRole.Title)
        AppText(text = rationale.message.resolve(), role = TextRole.Secondary)

        // Once the user has denied permanently the system dialog never appears again, so "Allow"
        // would be a button that does nothing. That is the whole reason PermissionStatus has four
        // cases rather than being a boolean.
        AppButton(
            label = (if (canAskAgain) rationale.actionLabel else rationale.settingsLabel).resolve(),
            onClick = {
                if (canAskAgain) request.request() else onEvent(ProfileEvent.OpenAppSettingsClicked)
            },
            kind = ButtonKind.Outline,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_cameraPermissionButton"),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    CameraRationale(
        rationale = permissionRationale(
            title = R.string.profile_camera_rationale_title.toUiText(),
            message = R.string.profile_camera_rationale_message.toUiText(),
        ),
        request = rememberPermissionRequest(Manifest.permission.CAMERA),
        onEvent = {},
    )
}
