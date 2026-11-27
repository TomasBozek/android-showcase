package com.example.androidproject1.service.session

import com.example.androidproject1.service.network.HttpClientFactory
import com.example.androidproject1.service.network.NetworkConfig
import com.example.androidproject1.service.network.Tokens
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultTokenRefresherTest {

    private var body = ""

    private fun refresher(answer: () -> HttpStatusCode, json: String = PAIR) = DefaultTokenRefresher(
        HttpClientFactory.create(
            engine = MockEngine { request ->
                body = request.body.toByteArray().decodeToString()
                val status = answer()
                if (status.value >= 400) {
                    respondError(status)
                } else {
                    respond(json, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                }
            },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
        ),
    )

    @Test
    fun `a good refresh token is exchanged for the new pair`() = runTest {
        val tokens = refresher({ HttpStatusCode.OK }).refresh("refresh-1")

        assertEquals(Tokens("access-2", "refresh-2"), tokens)
        assertEquals("""{"refreshToken":"refresh-1"}""", body)
    }

    @Test
    fun `a refusal is null, not an error`() = runTest {
        assertNull(refresher({ HttpStatusCode.Unauthorized }).refresh("spent"))
        assertNull(refresher({ HttpStatusCode.BadRequest }).refresh("spent"))
    }

    @Test
    fun `a server error is thrown, so a bad day does not end the session`() = runTest {
        val thrown = runCatching { refresher({ HttpStatusCode.ServiceUnavailable }).refresh("refresh-1") }

        assertTrue(thrown.exceptionOrNull() is ServerResponseException)
    }

    @Test
    fun `a transport failure is thrown, so a tunnel does not end the session`() = runTest {
        val client = HttpClientFactory.create(
            engine = MockEngine { throw IOException("no route") },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
        )

        val thrown = runCatching { DefaultTokenRefresher(client).refresh("refresh-1") }

        assertTrue(thrown.exceptionOrNull() is IOException)
    }

    private companion object {

        const val PAIR = """{"accessToken":"access-2","refreshToken":"refresh-2"}"""
    }
}
