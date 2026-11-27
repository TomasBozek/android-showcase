package com.example.androidproject1.feature.settings.domain.test

import com.example.androidproject1.feature.settings.domain.DownloadState
import com.example.androidproject1.feature.settings.domain.SampleDownload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [SampleDownload] a test drives: [start] puts it at zero and the test moves it on with
 * [progress] and [finish], as the job would; [calls] is what was asked for, in order.
 */
class FakeSampleDownload(initial: DownloadState = DownloadState.Idle) : SampleDownload {

    private val current = MutableStateFlow(initial)
    val calls = mutableListOf<String>()

    override val state: Flow<DownloadState> = current

    override fun start() {
        calls += "start"
        current.value = DownloadState.Running(0, TOTAL)
    }

    override fun cancel() {
        calls += "cancel"
        current.value = DownloadState.Idle
    }

    override suspend fun delete() {
        calls += "delete"
        current.value = DownloadState.Idle
    }

    fun progress(bytes: Long) {
        current.value = DownloadState.Running(bytes, TOTAL)
    }

    fun finish() {
        current.value = DownloadState.Done(TOTAL)
    }

    fun fail() {
        current.value = DownloadState.Failed
    }

    companion object {

        const val TOTAL = 100L * 1024 * 1024
    }
}
