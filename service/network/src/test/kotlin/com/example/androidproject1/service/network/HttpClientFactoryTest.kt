package com.example.androidproject1.service.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the client tells the session about: a `401`, and nothing else. */
class HttpClientFactoryTest {

    private val ender = object : SessionEnder {
        var count = 0

        override suspend fun end(reason: EndReason) {
            count++
        }
    }

    private fun clientAnswering(status: HttpStatusCode) = HttpClientFactory.create(
        engine = MockEngine { if (status.value >= 400) respondError(status) else respond("", status) },
        config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
        sessionEnder = ender,
    )

    private suspend fun call(status: HttpStatusCode) {
        runCatching { clientAnswering(status).get("things") }
    }

    @Test
    fun `a 401 signals that the session is over`() = runTest {
        call(HttpStatusCode.Unauthorized)

        assertEquals(1, ender.count)
    }

    @Test
    fun `a 403 does not — the session is fine, the thing is not theirs`() = runTest {
        call(HttpStatusCode.Forbidden)

        assertEquals(0, ender.count)
    }

    @Test
    fun `neither does a success or a server error`() = runTest {
        call(HttpStatusCode.OK)
        call(HttpStatusCode.InternalServerError)

        assertEquals(0, ender.count)
    }

    // Ktor answers a 401 with a refresh only when the server says which scheme it wants.
    private val challenge = headersOf(HttpHeaders.WWWAuthenticate, "Bearer")

    private class Attempt(val authorization: String?, val host: String)

    /**
     * A client whose server answers [answers] in turn (a request's status, `null` for 200), with a
     * store holding `stale` and a refresher that hands out `fresh` — or nothing, when [refresh] is
     * `null`. Every request the engine sees, refresh included, lands in [attempts].
     */
    private fun tokenClient(
        answers: List<HttpStatusCode>,
        refresh: Tokens? = Tokens("fresh", "refresh-2"),
        attempts: MutableList<Attempt>,
        store: FakeTokenStore = FakeTokenStore(access = "stale", refresh = "refresh-1"),
    ): io.ktor.client.HttpClient {
        var served = 0
        return HttpClientFactory.create(
            engine = MockEngine { request ->
                attempts += Attempt(request.headers[HttpHeaders.Authorization], request.url.host)
                val status = answers.getOrElse(served++) { HttpStatusCode.OK }
                if (status.value >= 400) respondError(status, headers = challenge) else respond("", status)
            },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
            tokenStore = store,
            tokenRefresher = object : TokenRefresher {
                override suspend fun refresh(refreshToken: String): Tokens? = refresh
            },
            sessionEnder = ender,
        )
    }

    @Test
    fun `a 401 is refreshed and the retried call carries the new token`() = runTest {
        val attempts = mutableListOf<Attempt>()
        val client = tokenClient(listOf(HttpStatusCode.Unauthorized), attempts = attempts)

        client.get("things")

        assertEquals(listOf("Bearer stale", "Bearer fresh"), attempts.map { it.authorization })
        assertEquals(0, ender.count)
    }

    @Test
    fun `a 401 after a refresh ends the session once`() = runTest {
        val attempts = mutableListOf<Attempt>()
        val client = tokenClient(
            answers = listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Unauthorized),
            attempts = attempts,
        )

        runCatching { client.get("things") }

        assertEquals(2, attempts.size)
        assertEquals(1, ender.count)
    }

    @Test
    fun `a refresh the server refuses ends the session once`() = runTest {
        val attempts = mutableListOf<Attempt>()
        val client = tokenClient(listOf(HttpStatusCode.Unauthorized), refresh = null, attempts = attempts)

        runCatching { client.get("things") }

        assertEquals(1, attempts.size)
        assertEquals(1, ender.count)
    }

    @Test
    fun `each request carries the token the store holds now, so a sign-out is not undone`() = runTest {
        val attempts = mutableListOf<Attempt>()
        val store = FakeTokenStore(access = "ada", refresh = "refresh-ada")
        val client = tokenClient(emptyList(), attempts = attempts, store = store)

        client.get("things")
        store.replace(accessToken = null, refreshToken = null)
        client.get("things")
        store.replace(accessToken = "bob", refreshToken = "refresh-bob")
        client.get("things")

        assertEquals(listOf("Bearer ada", null, "Bearer bob"), attempts.map { it.authorization })
    }

    @Test
    fun `the token is sent to the api's host and no other`() = runTest {
        val attempts = mutableListOf<Attempt>()
        val client = tokenClient(emptyList(), attempts = attempts)

        client.get("things")
        client.get("https://third-party.test/movie/1")

        assertEquals(
            listOf("example.test" to "Bearer stale", "third-party.test" to null),
            attempts.map {
                it.host to
                    it.authorization
            },
        )
    }

    @Test
    fun `a 401 from another host is not refreshed with the session's token`() = runTest {
        val attempts = mutableListOf<Attempt>()
        val client = tokenClient(listOf(HttpStatusCode.Unauthorized), attempts = attempts)

        runCatching { client.get("https://third-party.test/movie/1") }

        assertTrue(attempts.none { it.authorization != null })
    }

    @Test
    fun `a log sink hears each request, with no logger bound`() = runTest {
        val lines = mutableListOf<String>()
        val client = HttpClientFactory.create(
            engine = MockEngine { respond("", HttpStatusCode.OK) },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
            logSink = { lines += it },
        )

        client.get("things")

        assertTrue(lines.joinToString("\n"), lines.any { "https://example.test/things" in it })
    }

    @Test
    fun `an exchange is reported with its status and duration`() = runTest {
        val exchanges = mutableListOf<HttpExchange>()
        val client = HttpClientFactory.create(
            engine = MockEngine { respond("""{"ok":true}""", HttpStatusCode.Created) },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
            onExchange = { exchanges += it },
        )

        client.post("things") { setBody("""{"name":"tea"}""") }

        val exchange = exchanges.single()
        assertEquals("POST", exchange.method)
        assertEquals("https://example.test/things", exchange.url)
        assertEquals(201, exchange.status)
        assertTrue(exchange.durationMs >= 0)
        assertEquals("""{"name":"tea"}""", exchange.requestBody)
        assertEquals("""{"ok":true}""", exchange.responseBody)
    }

    @Test
    fun `an exchange that failed is reported, and the caller still reads its error`() = runTest {
        val exchanges = mutableListOf<HttpExchange>()
        val client = HttpClientFactory.create(
            engine = MockEngine { respondError(HttpStatusCode.NotFound, "gone") },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
            onExchange = { exchanges += it },
        )

        val failure = runCatching { client.get("things") }.exceptionOrNull()

        assertTrue(failure is io.ktor.client.plugins.ClientRequestException)
        assertEquals(404, exchanges.single().status)
        assertEquals("gone", exchanges.single().responseBody)
    }

    @Test
    fun `an exchange is reported without the Authorization header`() = runTest {
        val exchanges = mutableListOf<HttpExchange>()
        val store = FakeTokenStore(access = "secret-access", refresh = "secret-refresh")
        val client = HttpClientFactory.create(
            engine = MockEngine { respond("{}", HttpStatusCode.OK) },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
            tokenStore = store,
            tokenRefresher = object : TokenRefresher {
                override suspend fun refresh(refreshToken: String): Tokens? = null
            },
            onExchange = { exchanges += it },
        )

        client.get("things")

        assertEquals(1, exchanges.size)
        assertTrue(exchanges.single().toString(), "secret" !in exchanges.single().toString())
    }

    private val identity = RequestIdentity(
        applicationId = "com.example.app",
        versionName = "1.8.0",
        versionCode = 412,
        osVersion = "Android 15",
        model = "Google Pixel 9",
        language = { "cs-CZ" },
        newRequestId = { "req-${++minted}" },
    )
    private var minted = 0

    @Test
    fun `every request carries the app, the language and an id of its own`() = runTest {
        val seen = mutableListOf<io.ktor.http.Headers>()
        val client = HttpClientFactory.create(
            engine = MockEngine { request ->
                seen += request.headers
                respond("", HttpStatusCode.OK)
            },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
            identity = identity,
        )

        client.get("things")
        client.get("https://third-party.test/movie/1")

        assertEquals("com.example.app/1.8.0 (412; Android 15; Google Pixel 9)", seen[0][HttpHeaders.UserAgent])
        assertEquals("cs-CZ", seen[0][HttpHeaders.AcceptLanguage])
        assertEquals(listOf("req-1", "req-2"), seen.map { it["X-Request-Id"] })
    }

    @Test
    fun `a retried request keeps its id, and a failure reports it`() = runTest {
        val ids = mutableListOf<String?>()
        val failed = mutableListOf<String>()
        val client = HttpClientFactory.create(
            engine = MockEngine { request ->
                ids += request.headers["X-Request-Id"]
                respondError(HttpStatusCode.ServiceUnavailable)
            },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 1),
            identity = identity,
            onFailure = { failed += it },
        )

        runCatching { client.get("things") }

        assertEquals(listOf("req-1", "req-1"), ids)
        assertEquals(listOf("req-1"), failed)
    }

    @Test
    fun `a transport failure reports its id too`() = runTest {
        val failed = mutableListOf<String>()
        val client = HttpClientFactory.create(
            engine = MockEngine { throw java.io.IOException("no route") },
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
            identity = identity,
            onFailure = { failed += it },
        )

        runCatching { client.get("things") }

        assertEquals(listOf("req-1"), failed)
    }
}
