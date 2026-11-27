package com.example.androidproject1.feature.trips.presentation.component

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.choice.AppSwitch
import com.example.androidproject1.feature.trips.presentation.R
import com.example.androidproject1.service.core.ui.component.openAppSettings
import com.example.androidproject1.service.permission.PermissionStatus
import com.example.androidproject1.service.permission.rememberPermissionRequest

/**
 * The trip's reminder switch. Turning it **on** asks for `POST_NOTIFICATIONS` the first time
 * (API 33+; below it the permission is not a thing), through the permission request, and turns the
 * reminder on once it is granted. Refused for good, the switch sends the user to the app's settings
 * — the only place left to grant it — rather than sitting there doing nothing.
 */
@Composable
fun TripReminderSwitch(
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notifications = rememberPermissionRequest(Manifest.permission.POST_NOTIFICATIONS)
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val granted = !needsPermission || notifications.status == PermissionStatus.Granted
    // Set when a tap on "on" had to ask first, so the grant that follows completes it.
    var pending by remember { mutableStateOf(false) }

    LaunchedEffect(granted, pending) {
        if (pending && granted) {
            pending = false
            onToggle(true)
        }
    }

    AppSwitch(
        checked = on,
        onCheckedChange = { wanted ->
            when {
                !wanted || granted -> onToggle(wanted)
                notifications.status == PermissionStatus.RefusedForGood -> context.openAppSettings()
                else -> {
                    pending = true
                    notifications.request()
                }
            }
        },
        label = stringResource(R.string.trip_detail_reminder),
        supporting = stringResource(R.string.trip_detail_reminder_supporting),
        modifier = modifier.testTag("tripDetail_reminderSwitch"),
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    TripReminderSwitch(on = true, onToggle = {})
}
