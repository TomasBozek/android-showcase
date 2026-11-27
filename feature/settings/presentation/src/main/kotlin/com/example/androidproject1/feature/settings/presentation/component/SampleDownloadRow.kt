package com.example.androidproject1.feature.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.status.AppIndeterminateProgress
import com.example.androidproject1.core.ui.component.status.AppProgress
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.settings.domain.DownloadState
import com.example.androidproject1.feature.settings.presentation.R

/**
 * The sample file and what can be done with it right now: download it, watch it come down and
 * cancel, or delete it once it is here. The row keeps no state; [download] is what the job said.
 */
@Composable
fun SampleDownloadRow(
    download: DownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.inset.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
    ) {
        AppText(text = stringResource(R.string.settings_storage_sample), role = TextRole.Title)
        when (download) {
            DownloadState.Idle, DownloadState.Failed -> {
                if (download == DownloadState.Failed) {
                    AppText(
                        text = stringResource(R.string.settings_storage_failed),
                        modifier = Modifier.testTag("settingsStorage_failedValue"),
                    )
                }
                AppButton(
                    label = stringResource(R.string.settings_storage_download),
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth().testTag("settingsStorage_downloadButton"),
                )
            }

            is DownloadState.Running -> {
                val total = download.total
                if (total != null && total > 0) {
                    AppProgress(
                        fraction = download.bytes.toFloat() / total,
                        label = stringResource(
                            R.string.settings_storage_progress,
                            download.bytes.inMegabytes(),
                            total.inMegabytes(),
                        ),
                        modifier = Modifier.testTag("settingsStorage_downloadProgress"),
                    )
                } else {
                    AppIndeterminateProgress(modifier = Modifier.testTag("settingsStorage_downloadProgress"))
                }
                AppButton(
                    label = stringResource(R.string.settings_storage_cancel),
                    onClick = onCancel,
                    kind = ButtonKind.Outline,
                    modifier = Modifier.fillMaxWidth().testTag("settingsStorage_cancelButton"),
                )
            }

            is DownloadState.Done -> {
                AppText(
                    text = stringResource(R.string.settings_storage_done, download.bytes.inMegabytes()),
                    modifier = Modifier.testTag("settingsStorage_doneValue"),
                )
                AppButton(
                    label = stringResource(R.string.settings_storage_delete),
                    onClick = onDelete,
                    kind = ButtonKind.Destructive,
                    modifier = Modifier.fillMaxWidth().testTag("settingsStorage_deleteButton"),
                )
            }
        }
    }
}

/** Whole megabytes, in the units the phone's own Settings uses for a file. */
private fun Long.inMegabytes(): Long = this / (1024 * 1024)

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    SampleDownloadRow(
        download = DownloadState.Running(42L * 1024 * 1024, 100L * 1024 * 1024),
        onDownload = {},
        onCancel = {},
        onDelete = {},
    )
}
