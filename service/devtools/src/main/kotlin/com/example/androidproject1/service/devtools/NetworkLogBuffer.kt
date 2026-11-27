package com.example.androidproject1.service.devtools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URI
import java.util.concurrent.atomic.AtomicLong

/**
 * The last [capacity] exchanges the HTTP client made, newest first — what the dev menu's network
 * log lists when a request went wrong and nobody had logcat open.
 *
 * Fed by `HttpClientFactory.create(onExchange = …)` through `:core:di`, so `:service:network`
 * never depends on this module and this one never on it. Kept in memory only: a process that dies
 * takes its log with it, and a build that binds no buffer records nothing.
 */
public class NetworkLogBuffer(private val capacity: Int = DEFAULT_CAPACITY) {

    init {
        require(capacity > 0) { "capacity must be positive, was $capacity" }
    }

    private val ids = AtomicLong()
    private val mutableEntries = MutableStateFlow<List<NetworkLogEntry>>(emptyList())

    /** Newest first. */
    public val entries: StateFlow<List<NetworkLogEntry>> = mutableEntries.asStateFlow()

    public fun record(
        method: String,
        url: String,
        status: Int?,
        durationMs: Long,
        requestBody: String?,
        responseBody: String?,
    ) {
        val entry = NetworkLogEntry(
            id = ids.incrementAndGet(),
            method = method,
            url = url,
            path = runCatching { URI(url).rawPath }.getOrNull()?.ifEmpty { "/" } ?: url,
            status = status,
            durationMs = durationMs,
            requestBody = requestBody,
            responseBody = responseBody,
        )
        mutableEntries.update { (listOf(entry) + it).take(capacity) }
    }

    /**
     * The `X-Request-Id` of the last request that failed, what a support ticket quotes so
     * the server's log can be searched for it; `null` until one has. Fed by
     * `HttpClientFactory.create(onFailure = buffer::recordFailure)`.
     */
    @Volatile
    public var lastFailedRequestId: String? = null
        private set

    public fun recordFailure(requestId: String) {
        lastFailedRequestId = requestId
    }

    public fun clear() {
        mutableEntries.value = emptyList()
    }

    public companion object {
        public const val DEFAULT_CAPACITY: Int = 200
    }
}

/**
 * One exchange as the log shows it: [path] for the row, [url] and the bodies for the detail.
 *
 * @property status `null` when no answer came back.
 */
public data class NetworkLogEntry(
    val id: Long,
    val method: String,
    val url: String,
    val path: String,
    val status: Int?,
    val durationMs: Long,
    val requestBody: String?,
    val responseBody: String?,
)
