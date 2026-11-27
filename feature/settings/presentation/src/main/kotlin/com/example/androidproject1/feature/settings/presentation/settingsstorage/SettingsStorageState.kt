package com.example.androidproject1.feature.settings.presentation.settingsstorage

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.androidproject1.feature.settings.domain.DownloadState

/** The sample file's download, as the job last reported it. */
@Immutable
data class SettingsStorageState(
    val download: DownloadState = DownloadState.Idle,
) {

    companion object {

        val PREVIEW = SettingsStorageState()
    }
}

/** One state per thing the row can be doing: idle, part way, unknown size, done, failed. */
class SettingsStorageStatePreviews : PreviewParameterProvider<SettingsStorageState> {

    private val mb = 1024L * 1024

    override val values = sequenceOf(
        SettingsStorageState.PREVIEW,
        SettingsStorageState(DownloadState.Running(42 * mb, 100 * mb)),
        SettingsStorageState(DownloadState.Running(3 * mb, null)),
        SettingsStorageState(DownloadState.Done(100 * mb)),
        SettingsStorageState(DownloadState.Failed),
    )
}
