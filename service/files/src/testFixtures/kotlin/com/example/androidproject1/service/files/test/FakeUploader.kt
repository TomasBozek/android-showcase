package com.example.androidproject1.service.files.test

import com.example.androidproject1.service.files.UploadProgress
import com.example.androidproject1.service.files.Uploader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/** An [Uploader] that plays [script] for every call, and records the calls. */
class FakeUploader(var script: List<UploadProgress> = listOf(UploadProgress.Done)) : Uploader {

    val requested = mutableListOf<Triple<File, String, String>>()

    override fun upload(file: File, url: String, contentType: String): Flow<UploadProgress> {
        requested += Triple(file, url, contentType)
        return flow { script.forEach { emit(it) } }
    }
}
