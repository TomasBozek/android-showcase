package com.example.androidproject1.feature.settings.data.download

import com.example.androidproject1.feature.settings.domain.DownloadState
import com.example.androidproject1.service.files.FilePurpose
import com.example.androidproject1.service.files.FileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the download is, held in memory where the job that does it and the screen that shows it
 * can both reach it. It starts from the disk — a finished file is [DownloadState.Done] after a
 * restart — and from then on it is what the job last said.
 */
class SampleDownloadBoard(files: FileStore, config: SampleDownloadConfig) {

    private val target = files.file(FilePurpose.Downloads, config.fileName)
    private val current = MutableStateFlow<DownloadState>(
        if (target.exists()) DownloadState.Done(target.length()) else DownloadState.Idle,
    )

    val state: StateFlow<DownloadState> = current.asStateFlow()

    fun set(state: DownloadState) {
        current.value = state
    }
}
