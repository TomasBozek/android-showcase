package com.example.androidproject1.feature.settings.data.download

import com.example.androidproject1.feature.settings.domain.DownloadState
import com.example.androidproject1.service.files.DownloadProgress
import com.example.androidproject1.service.files.FilePurpose
import com.example.androidproject1.service.files.test.FakeDownloader
import com.example.androidproject1.service.files.test.FakeFiles
import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.NotificationProgress
import com.example.androidproject1.service.notification.test.FakeNotifier
import com.example.androidproject1.service.work.NetworkRequirement
import com.example.androidproject1.service.work.WorkResult
import com.example.androidproject1.service.work.test.FakeScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The download end to end but for the network: the port schedules a named job, the job streams
 * through a scripted downloader and reports to the board and the notification, and cancel and
 * delete leave nothing behind — real files in a temporary folder, fakes for the rest.
 */
class SampleDownloadTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val config = SampleDownloadConfig(url = "https://files.example/big.bin", fileName = "big.bin")
    private val notifier = FakeNotifier()
    private val total = 100L * 1024 * 1024

    private val files by lazy { FakeFiles(folder.root) }
    private val downloader = FakeDownloader()
    private val board by lazy { SampleDownloadBoard(files, config) }
    private val handler by lazy { SampleDownloadHandler(downloader, files, notifier, board, config) }
    private val scheduler by lazy { FakeScheduler(listOf(handler)) }
    private val download by lazy { DefaultSampleDownload(scheduler, files, notifier, board, config) }

    private fun percent(fraction: Int) = DownloadProgress.Running(total * fraction / 100, total)

    @Test
    fun `start schedules one named job that needs a network and keeps a running one`() = runTest {
        downloader.script = listOf(percent(50))

        download.start()

        val spec = scheduler.scheduled.single()
        assertEquals(SampleDownloadHandler.NAME, spec.name)
        assertEquals(SampleDownloadHandler.KEY, spec.handler)
        assertEquals(NetworkRequirement.Connected, spec.constraints.network)
        assertFalse(spec.replace)
    }

    @Test
    fun `progress reaches the board and the notification, then done`() = runTest {
        val target = files.file(FilePurpose.Downloads, "big.bin")
        downloader.script = listOf(percent(10), percent(50), DownloadProgress.Done(target))

        download.start()

        assertEquals(WorkResult.Success, scheduler.results[SampleDownloadHandler.NAME])
        assertEquals(DownloadState.Done(target.length()), download.state.first())
        assertEquals(config.url, downloader.requested.single().first)
        val running = notifier.posted.filter { it.progress != null }
        assertEquals(
            listOf(NotificationProgress(total / 10, total), NotificationProgress(total / 2, total)),
            running.map { it.progress },
        )
        assertTrue(running.all { it.kind == NotificationKind.Downloads })
        assertNull("the last one is the plain 'complete', no bar", notifier.posted.last().progress)
    }

    @Test
    fun `a notification is posted once per percent, not once per chunk`() = runTest {
        downloader.script = List(50) { DownloadProgress.Running(total / 10_000 * (it + 1), total) }

        download.start()

        assertTrue("at most one for each percent the file moved: ${notifier.posted.size}", notifier.posted.size <= 1)
    }

    @Test
    fun `a failure is on the board and in the shade, and the job is not retried`() = runTest {
        downloader.script = listOf(percent(30), DownloadProgress.Failed(RuntimeException("offline")))

        download.start()

        assertEquals(DownloadState.Failed, download.state.first())
        assertEquals(WorkResult.Failure, scheduler.results[SampleDownloadHandler.NAME])
        assertNull(notifier.posted.last().progress)
    }

    @Test
    fun `starting again after a failure asks the downloader again, which resumes what is kept`() = runTest {
        downloader.script = listOf(DownloadProgress.Failed(RuntimeException("offline")))
        download.start()

        download.start()

        assertEquals(2, downloader.requested.size)
    }

    @Test
    fun `a finished file is not fetched again`() = runTest {
        files.file(FilePurpose.Downloads, "big.bin").writeText("abc")

        download.start()

        assertEquals(DownloadState.Done(3), download.state.first())
        assertTrue(downloader.requested.isEmpty())
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `cancel stops the job, clears its notification and forgets the part`() = runTest {
        download.start()
        val part = files.file(FilePurpose.Downloads, "big.bin.part").apply { writeText("ab") }

        download.cancel()

        assertFalse(part.exists())
        assertEquals(listOf(SampleDownloadHandler.NAME), scheduler.cancelled)
        assertEquals(listOf(SampleDownloadHandler.NOTIFICATION_ID), notifier.cancelled)
        assertEquals(DownloadState.Idle, download.state.first())
    }

    @Test
    fun `delete removes the file and any part, and is idle again`() = runTest {
        val target = files.file(FilePurpose.Downloads, "big.bin").apply { writeText("abc") }
        val part = files.file(FilePurpose.Downloads, "big.bin.part").apply { writeText("ab") }

        download.delete()

        assertFalse(target.exists())
        assertFalse(part.exists())
        assertEquals(DownloadState.Idle, download.state.first())
    }
}
