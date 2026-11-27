package com.example.androidproject1.service.files

import com.example.androidproject1.service.network.HttpErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * [Downloader] over the app's Ktor client.
 *
 * **Resumes by `Range`.** What an earlier try wrote is `<name>.part`; a new one asks for
 * `Range: bytes=<its length>-` and appends what comes back. A server that ignores the header and
 * answers `200` gets the file from the start, over the top of the old bytes — never appended to
 * them. A `416` means the bytes on disk are the whole file.
 *
 * The client's request timeout is for an API call and would end a large download, so a download
 * has none; the socket timeout stays, so a stalled connection still fails.
 */
public class KtorDownloader(private val client: HttpClient) : Downloader {

    override fun download(url: String, to: File): Flow<DownloadProgress> = channelFlow {
        try {
            send(fetch(url, part = File(to.parentFile, to.name + PART), to = to))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            send(DownloadProgress.Failed(HttpErrorMapper.map(failure)))
        }
    }.flowOn(Dispatchers.IO)

    /** The terminal answer; the [DownloadProgress.Running] ones are sent on the way. */
    private suspend fun ProducerScope<DownloadProgress>.fetch(url: String, part: File, to: File): DownloadProgress {
        val have = if (part.exists()) part.length() else 0L
        return try {
            client.prepareGet(url) {
                timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
                if (have > 0) header(HttpHeaders.Range, "bytes=$have-")
            }.execute { response -> receive(response, have, part, to) }
        } catch (refused: ClientRequestException) {
            // The range starts where the file ends: nothing left to send, so there is nothing to do.
            if (refused.response.status != HttpStatusCode.RequestedRangeNotSatisfiable || have == 0L) throw refused
            place(part, to)
            DownloadProgress.Done(to)
        }
    }

    private suspend fun ProducerScope<DownloadProgress>.receive(
        response: HttpResponse,
        have: Long,
        part: File,
        to: File,
    ): DownloadProgress {
        // A 200 to a request that asked for a range is the whole file again.
        val resuming = response.status == HttpStatusCode.PartialContent
        val start = if (resuming) have else 0L
        val total = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()?.let { it + start }
        var written = start
        send(DownloadProgress.Running(written, total))

        val channel = response.bodyAsChannel()
        val buffer = ByteArray(BUFFER)
        FileOutputStream(part, resuming).use { out ->
            var reported = written
            while (true) {
                val read = channel.readAvailable(buffer, 0, buffer.size)
                if (read < 0) break
                out.write(buffer, 0, read)
                written += read
                if (written - reported >= REPORT_EVERY) {
                    send(DownloadProgress.Running(written, total))
                    reported = written
                }
            }
        }
        // A connection that closes early without an error is still a short file.
        if (total != null && written != total) error("Got $written of $total bytes")
        send(DownloadProgress.Running(written, total))
        place(part, to)
        return DownloadProgress.Done(to)
    }

    private fun place(part: File, to: File) {
        to.delete()
        check(part.renameTo(to)) { "Could not move the download into place" }
    }

    private companion object {

        const val PART = ".part"
        const val BUFFER = 16 * 1024
        const val REPORT_EVERY = 64 * 1024
    }
}
