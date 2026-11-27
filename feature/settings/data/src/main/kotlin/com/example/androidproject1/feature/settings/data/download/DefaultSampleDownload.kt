package com.example.androidproject1.feature.settings.data.download

import com.example.androidproject1.feature.settings.domain.DownloadState
import com.example.androidproject1.feature.settings.domain.SampleDownload
import com.example.androidproject1.service.files.FilePurpose
import com.example.androidproject1.service.files.FileStore
import com.example.androidproject1.service.notification.Notifier
import com.example.androidproject1.service.work.NetworkRequirement
import com.example.androidproject1.service.work.Scheduler
import com.example.androidproject1.service.work.WorkConstraints
import com.example.androidproject1.service.work.WorkSpec
import kotlinx.coroutines.flow.Flow

/**
 * The port over the job: [start] schedules it (named, so a second tap while it runs keeps the one
 * running), [cancel] stops it and drops the part it had written, [delete] removes the finished file.
 */
class DefaultSampleDownload(
    private val scheduler: Scheduler,
    private val files: FileStore,
    private val notifier: Notifier,
    private val board: SampleDownloadBoard,
    private val config: SampleDownloadConfig,
) : SampleDownload {

    override val state: Flow<DownloadState> = board.state

    override fun start() {
        val now = board.state.value
        if (now is DownloadState.Running || now is DownloadState.Done) return
        board.set(DownloadState.Running(bytes = 0, total = null))
        scheduler.schedule(
            WorkSpec(
                name = SampleDownloadHandler.NAME,
                handler = SampleDownloadHandler.KEY,
                constraints = WorkConstraints(network = NetworkRequirement.Connected),
                replace = false,
            ),
        )
    }

    override fun cancel() {
        scheduler.cancel(SampleDownloadHandler.NAME)
        notifier.cancel(SampleDownloadHandler.NOTIFICATION_ID)
        // What had arrived is dropped, as the interface says: a cancelled download starts over.
        files.file(FilePurpose.Downloads, config.fileName + PART).delete()
        board.set(DownloadState.Idle)
    }

    override suspend fun delete() {
        scheduler.cancel(SampleDownloadHandler.NAME)
        notifier.cancel(SampleDownloadHandler.NOTIFICATION_ID)
        files.delete(files.file(FilePurpose.Downloads, config.fileName))
        files.delete(files.file(FilePurpose.Downloads, config.fileName + PART))
        board.set(DownloadState.Idle)
    }

    private companion object {

        /** The name the downloader gives what has arrived so far. */
        const val PART = ".part"
    }
}
