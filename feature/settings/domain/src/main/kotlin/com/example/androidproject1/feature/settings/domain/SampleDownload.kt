package com.example.androidproject1.feature.settings.domain

import kotlinx.coroutines.flow.Flow

/** Where the sample file's download is. What is on disk decides the first one a screen sees. */
sealed interface DownloadState {

    /** Nothing downloaded and nothing under way. */
    data object Idle : DownloadState

    /** [bytes] so far of [total], when the server said how many; a resumed download starts from what is kept. */
    data class Running(val bytes: Long, val total: Long?) : DownloadState

    /** The whole file is on disk, [bytes] of it. */
    data class Done(val bytes: Long) : DownloadState

    /** Stopped by an error; what arrived is kept and [SampleDownload.start] carries on from it. */
    data object Failed : DownloadState
}

/**
 * A large file the app can fetch and remove, to show what that looks like: progress on screen and
 * in a notification, cancel, resume and delete. The download itself is a job that runs with the
 * screen closed, so [state] is what the job reports and not what a screen last asked for.
 */
interface SampleDownload {

    val state: Flow<DownloadState>

    /** Starts the download, or carries on from what an earlier try kept. A running one is left alone. */
    fun start()

    /** Stops it and drops what had arrived. */
    fun cancel()

    /** Deletes the finished file. */
    suspend fun delete()
}
