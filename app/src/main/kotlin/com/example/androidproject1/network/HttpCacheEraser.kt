package com.example.androidproject1.network

import android.content.Context
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The application's share of "delete my account": the HTTP cache under `cacheDir/http`,
 * which `cachedOkHttpEngine` fills on `staging` and `prod` and the real-API switch on `dev`. A
 * cached answer about the account is account data like any other, and the directory is this
 * module's to name because the engine that writes it is.
 */
class HttpCacheEraser(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) : LocalDataEraser {

    override suspend fun erase() = withContext(dispatcherProvider.io) {
        File(context.cacheDir, HTTP_CACHE_DIRECTORY).deleteRecursively()
        Unit
    }
}

/** Where the OkHttp cache lives, spelled once for the engines and the eraser. */
const val HTTP_CACHE_DIRECTORY = "http"
