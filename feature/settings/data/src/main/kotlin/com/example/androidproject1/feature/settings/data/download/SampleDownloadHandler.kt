package com.example.androidproject1.feature.settings.data.download

import com.example.androidproject1.feature.settings.data.R
import com.example.androidproject1.feature.settings.domain.DownloadState
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.files.DownloadProgress
import com.example.androidproject1.service.files.Downloader
import com.example.androidproject1.service.files.FilePurpose
import com.example.androidproject1.service.files.FileStore
import com.example.androidproject1.service.notification.AppNotification
import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.NotificationProgress
import com.example.androidproject1.service.notification.Notifier
import com.example.androidproject1.service.work.WorkHandler
import com.example.androidproject1.service.work.WorkResult

/**
 * The job behind "Download sample file": streams the file, tells the board and the notification
 * how far it is, and says how it ended. A download that failed is [WorkResult.Failure] and not a
 * retry: the user starts it again from the screen, and what arrived is resumed.
 */
class SampleDownloadHandler(
    private val downloader: Downloader,
    private val files: FileStore,
    private val notifier: Notifier,
    private val board: SampleDownloadBoard,
    private val config: SampleDownloadConfig,
) : WorkHandler {

    override val key: String = KEY

    override suspend fun run(input: Map<String, String>): WorkResult {
        val target = files.file(FilePurpose.Downloads, config.fileName)
        if (target.exists()) {
            board.set(DownloadState.Done(target.length()))
            return WorkResult.Success
        }
        var result = WorkResult.Failure
        var shown = -1L
        downloader.download(config.url, target).collect { progress ->
            when (progress) {
                is DownloadProgress.Running -> {
                    board.set(DownloadState.Running(progress.bytes, progress.total))
                    val step = step(progress.bytes, progress.total)
                    // Every whole percent, not every chunk: the shade cannot draw them faster anyway.
                    if (step != shown) {
                        shown = step
                        notifier.post(running(progress.bytes, progress.total))
                    }
                }

                is DownloadProgress.Done -> {
                    board.set(DownloadState.Done(progress.file.length()))
                    notifier.post(finished(R.string.settings_storage_download_done))
                    result = WorkResult.Success
                }

                is DownloadProgress.Failed -> {
                    board.set(DownloadState.Failed)
                    notifier.post(finished(R.string.settings_storage_download_failed))
                }
            }
        }
        return result
    }

    private fun step(bytes: Long, total: Long?): Long =
        if (total != null && total > 0) bytes * PERCENT / total else bytes / MEGABYTE

    private fun running(bytes: Long, total: Long?) = AppNotification(
        kind = NotificationKind.Downloads,
        id = NOTIFICATION_ID,
        title = UiText.Resource(R.string.settings_storage_download_title),
        text = UiText.Resource(R.string.settings_storage_download_running),
        progress = NotificationProgress(bytes, total),
    )

    private fun finished(text: Int) = AppNotification(
        kind = NotificationKind.Downloads,
        id = NOTIFICATION_ID,
        title = UiText.Resource(R.string.settings_storage_download_title),
        text = UiText.Resource(text),
    )

    companion object {

        const val KEY = "sample-download"
        const val NAME = "sample-download"
        const val NOTIFICATION_ID = 7_100

        private const val PERCENT = 100L
        private const val MEGABYTE = 1024L * 1024
    }
}
