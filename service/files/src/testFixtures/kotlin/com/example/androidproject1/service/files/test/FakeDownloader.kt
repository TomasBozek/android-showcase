package com.example.androidproject1.service.files.test

import com.example.androidproject1.service.files.DownloadProgress
import com.example.androidproject1.service.files.Downloader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/** A [Downloader] that plays [script] for every call, and records the calls. */
class FakeDownloader(var script: List<DownloadProgress> = emptyList()) : Downloader {

    val requested = mutableListOf<Pair<String, File>>()

    override fun download(url: String, to: File): Flow<DownloadProgress> {
        requested += url to to
        return flow { script.forEach { emit(it) } }
    }
}
