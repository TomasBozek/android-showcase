package com.example.androidproject1.service.network

/**
 * One request and its answer as the wire saw them, for the dev menu's network log.
 *
 * Headers are not in it, so the bearer token cannot be: `Authorization` is the one header worth
 * logging and the one that must never be. The bodies are kept as text, cut at [MAX_BODY] characters.
 *
 * @property status `null` when no answer came back — a timeout, no route, a cancelled call.
 * @property durationMs from the request leaving to its answer arriving, retries counted apart.
 */
public data class HttpExchange(
    val method: String,
    val url: String,
    val status: Int?,
    val durationMs: Long,
    val requestBody: String?,
    val responseBody: String?,
) {

    public companion object {
        public const val MAX_BODY: Int = 64 * 1024
    }
}
