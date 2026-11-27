package com.example.androidproject1.service.files

import com.example.androidproject1.service.network.HttpErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * [Uploader] over the app's Ktor client: the file is streamed from disk as the one part of a
 * multipart body, never read whole, and Ktor's `onUpload` says how far it got.
 *
 * Like a download, an upload has no request timeout — a slow uplink is not a dead one — and keeps
 * the socket timeout, so a stalled connection still fails.
 */
public class KtorUploader(private val client: HttpClient) : Uploader {

    override fun upload(file: File, url: String, contentType: String): Flow<UploadProgress> = channelFlow {
        try {
            send(UploadProgress.Sending(0f))
            put(file, url, contentType)
            send(UploadProgress.Sending(1f))
            send(UploadProgress.Done)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            send(UploadProgress.Failed(HttpErrorMapper.map(failure)))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun ProducerScope<UploadProgress>.put(file: File, url: String, contentType: String) {
        val size = file.length()
        val body = MultiPartFormDataContent(
            formData {
                append(
                    key = PART,
                    value = InputProvider(size) { file.inputStream().asInput() },
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                    },
                )
            },
        )
        var reported = 0f
        client.put(url) {
            timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
            setBody(body)
            onUpload { sent, total ->
                // The multipart total counts the boundaries as well, so it is the one to divide by.
                val whole = total ?: return@onUpload
                if (whole <= 0) return@onUpload
                val fraction = (sent.toFloat() / whole).coerceIn(0f, 1f)
                // A step of a hundredth at most: a fast link would otherwise send thousands.
                if (fraction - reported >= STEP && fraction < 1f) {
                    reported = fraction
                    send(UploadProgress.Sending(fraction))
                }
            }
        }
    }

    private companion object {

        const val PART = "file"
        const val STEP = 0.01f
    }
}
