package com.example.androidproject1.feature.settings.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.status.AppTag
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.feature.settings.presentation.R
import com.example.androidproject1.feature.settings.presentation.permissions.PermissionRow
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.resolve

/** One declared permission and whether the user has granted it. */
@Composable
fun PermissionListItem(row: PermissionRow, modifier: Modifier = Modifier) {
    AppListItem(
        modifier = modifier.testTag("settingsPermissions_permissionItem"),
        headline = row.label.resolve(),
        supporting = row.name,
        trailing = {
            AppTag(
                label = if (row.isGranted) {
                    stringResource(R.string.settings_permissions_granted)
                } else {
                    stringResource(R.string.settings_permissions_denied)
                },
                tone = if (row.isGranted) TagTone.Positive else TagTone.Negative,
            )
        },
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    PermissionListItem(
        row = PermissionRow(
            name = "android.permission.INTERNET",
            label = UiText.Literal("INTERNET"),
            isGranted = true,
        ),
    )
    PermissionListItem(
        row = PermissionRow(
            name = "android.permission.POST_NOTIFICATIONS",
            label = UiText.Literal("Notifications"),
            isGranted = false,
        ),
    )
}
