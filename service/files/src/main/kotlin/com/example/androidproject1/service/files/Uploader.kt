package com.example.androidproject1.service.files

import kotlinx.coroutines.flow.Flow
import java.io.File

/** How an upload is going. A flow of these ends at [Done] or [Failed]; cancelling it cancels the upload. */
public sealed interface UploadProgress {

    /** [fraction] of the file sent, `0f` to `1f`. */
    public data class Sending(val fraction: Float) : UploadProgress

    public data object Done : UploadProgress

    /** [error] is a `DomainError` where the network said what went wrong. Nothing is resumed: a retry sends it all again. */
    public data class Failed(val error: Throwable) : UploadProgress
}

public interface Uploader {

    /** Sends [file] to [url] as one part of a multipart `PUT`, typed [contentType]. */
    public fun upload(file: File, url: String, contentType: String): Flow<UploadProgress>
}
