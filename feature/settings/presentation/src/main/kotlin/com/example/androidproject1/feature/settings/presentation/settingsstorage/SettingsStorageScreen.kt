package com.example.androidproject1.feature.settings.presentation.settingsstorage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.feature.settings.presentation.R
import com.example.androidproject1.feature.settings.presentation.component.SampleDownloadRow

@Composable
fun SettingsStorageScreen(
    state: SettingsStorageState,
    onEvent: (SettingsStorageEvent) -> Unit,
) {
    AppScaffold(
        screenId = "SettingsStorageScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_storage_title),
                onNavigateUp = { onEvent(SettingsStorageEvent.NavigateUpClicked) },
                navigateUpTestTag = "settingsStorage_upButton",
            )
        },
        contentPadding = false,
    ) {
        SampleDownloadRow(
            download = state.download,
            onDownload = { onEvent(SettingsStorageEvent.DownloadClicked) },
            onCancel = { onEvent(SettingsStorageEvent.CancelClicked) },
            onDelete = { onEvent(SettingsStorageEvent.DeleteClicked) },
        )
    }
}

@ScreenPreview
@Composable
private fun Preview(
    @PreviewParameter(SettingsStorageStatePreviews::class) state: SettingsStorageState,
) = ThemedScreenPreview {
    SettingsStorageScreen(
        state = state,
    ) {}
}
