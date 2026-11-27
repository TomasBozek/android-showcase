package com.example.androidproject1.feature.devmenu.presentation.networklog

import androidx.compose.runtime.Immutable
import com.example.androidproject1.service.devtools.NetworkLogEntry

/**
 * The network log: every exchange the client made since start, newest first, and the one
 * whose sheet is open.
 */
@Immutable
data class NetworkLogState(
    val entries: List<NetworkLogEntry> = emptyList(),
    val selected: NetworkLogEntry? = null,
) {

    companion object {

        val PREVIEW = NetworkLogState(
            entries = listOf(
                NetworkLogEntry(
                    id = 3,
                    method = "POST",
                    url = "https://example.test/orders",
                    path = "/orders",
                    status = 201,
                    durationMs = 84,
                    requestBody = """{"lines":[{"productId":"coffee","quantity":2}]}""",
                    responseBody = """{"id":"1042"}""",
                ),
                NetworkLogEntry(
                    id = 2,
                    method = "GET",
                    url = "https://example.test/products/unknown",
                    path = "/products/unknown",
                    status = 404,
                    durationMs = 31,
                    requestBody = null,
                    responseBody = """{"message":"not found"}""",
                ),
                NetworkLogEntry(
                    id = 1,
                    method = "GET",
                    url = "https://api.tvmaze.com/shows?page=0",
                    path = "/shows",
                    status = null,
                    durationMs = 10_000,
                    requestBody = null,
                    responseBody = null,
                ),
            ),
        )
    }
}
