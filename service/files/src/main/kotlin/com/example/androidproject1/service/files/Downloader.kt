package com.example.androidproject1.service.files

import kotlinx.coroutines.flow.Flow
import java.io.File

/** How a download is going. A flow of these ends at [Done] or [Failed]; cancelling it cancels the download. */
public sealed interface DownloadProgress {

    /** [bytes] so far, of [total] when the server said how many. Counts what is already on disk when resuming. */
    public data class Running(val bytes: Long, val total: Long?) : DownloadProgress

    public data class Done(val file: File) : DownloadProgress

    /** [error] is a `DomainError` where the network said what went wrong. What arrived is kept, and the next call resumes it. */
    public data class Failed(val error: Throwable) : DownloadProgress
}

public interface Downloader {

    /**
     * Streams [url] into [to], resuming what an earlier try left. The bytes land beside it, in
     * `<name>.part`, and become [to] only when all of them have — so [to] existing means the
     * download is whole.
     */
    public fun download(url: String, to: File): Flow<DownloadProgress>
}
