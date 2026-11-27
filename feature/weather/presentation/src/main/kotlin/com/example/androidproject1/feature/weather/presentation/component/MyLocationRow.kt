package com.example.androidproject1.feature.weather.presentation.component

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.permission.PermissionGate
import com.example.androidproject1.service.permission.PermissionStatus
import com.example.androidproject1.service.permission.permissionRationale

/**
 * "Use my location", above the search.
 *
 * The button is composed only while `ACCESS_COARSE_LOCATION` is held — the gate's value is the
 * type, so there is no ungranted branch to forget. Otherwise the rationale: why the app wants it and
 * what it does with it, and the one button that can still do something — asking again while the
 * system will, the app's settings once it will not. Nothing asks at start: the first tap is the ask.
 */
@Composable
fun MyLocationRow(
    locating: Boolean,
    onUse: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rationale = permissionRationale(
        title = R.string.weather_my_location_rationale_title.toUiText(),
        message = R.string.weather_my_location_rationale_message.toUiText(),
    )
    // Set when the tap had to ask first: the grant that follows completes it, so the user does not
    // grant the permission and then have to tap the same button again.
    var pending by remember { mutableStateOf(false) }
    PermissionGate(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        rationale = rationale,
        modifier = modifier,
        denied = { request ->
            val canAskAgain = request.status !is PermissionStatus.RefusedForGood
            // The rationale is one line under the button, not a card: it sits above the search on
            // every open until the permission is held, and a card would push the list off a small
            // phone.
            Column(
                modifier = modifier.fillMaxWidth().testTag("weather_myLocationRationaleTile"),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
            ) {
                // Once the user has refused for good the system dialog never shows again, so
                // asking would be a button that does nothing: it opens the settings instead.
                AppButton(
                    label = stringResource(
                        if (canAskAgain) R.string.weather_use_my_location else R.string.weather_my_location_settings,
                    ),
                    onClick = {
                        if (canAskAgain) {
                            pending = true
                            request.request()
                        } else {
                            onOpenSettings()
                        }
                    },
                    kind = ButtonKind.Outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(
                            if (canAskAgain) {
                                "weather_myLocationPermissionButton"
                            } else {
                                "weather_myLocationSettingsButton"
                            },
                        ),
                )
                AppText(
                    text = stringResource(R.string.weather_my_location_rationale_message),
                    role = TextRole.Secondary,
                )
            }
        },
    ) {
        LaunchedEffect(Unit) {
            if (pending) {
                pending = false
                onUse()
            }
        }
        AppButton(
            label = stringResource(if (locating) R.string.weather_locating else R.string.weather_use_my_location),
            onClick = onUse,
            enabled = !locating,
            kind = ButtonKind.Outline,
            modifier = modifier
                .fillMaxWidth()
                .testTag("weather_myLocationButton"),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    MyLocationRow(locating = false, onUse = {}, onOpenSettings = {})
}
