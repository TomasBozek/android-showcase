package com.example.androidproject1.network

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The `dev` fixture engine, driven through a real client: the app's own routes, the knobs, and the
 * switch that turns every route into a `503`.
 *
 * The fixtures are read off the `dev` resources, which is why this runs under Robolectric — the
 * engine wants a `Context`, and a test that pasted the JSON in would pass on a fixture the app
 * cannot load. A plain [Application], not [com.example.androidproject1.App]: the engine needs the
 * resources, not the Koin graph, and a graph started per test is a graph started twice.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NetworkEngineTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** What the real half saw, and a canned answer so the test never leaves the machine. */
    private val forwarded = mutableListOf<String>()
    private val realClient = lazy {
        HttpClient(
            MockEngine { request ->
                forwarded += request.url.toString()
                respond(content = REAL_BODY, status = HttpStatusCode.OK, headers = headersOf("X-Real", "yes"))
            },
        )
    }

    private fun client() = HttpClient(networkEngine(context, realClient))
    private val client = client()

    @After
    fun tearDown() {
        FixtureNetwork.failingOverride = false
        FixtureNetwork.realApiOverride = false
        FixtureNetwork.latencyOverride = 0
        FixtureNetwork.failureRateOverride = 0
        FixtureNetwork.resetFailures()
        FixtureNetwork.sessionExpiryArmed = false
        FixtureNetwork.refreshRefused = false
        client.close()
    }

    @Test
    fun `the config holds nothing until a switch says so, and each switch is its field`() = runBlocking {
        assertEquals(
            """{"minVersionCode":0,"latestVersionCode":0,"maintenance":false}""",
            client.get("https://fixtures.test/config").bodyAsText(),
        )

        FixtureNetwork.setBelowMinimum(context, true)
        FixtureNetwork.setMaintenance(context, true)
        FixtureNetwork.setNewerAvailable(context, true)
        try {
            val body = Json.parseToJsonElement(client.get("https://fixtures.test/config").bodyAsText()).jsonObject
            assertEquals(Int.MAX_VALUE, body.getValue("minVersionCode").jsonPrimitive.content.toInt())
            assertEquals(Int.MAX_VALUE, body.getValue("latestVersionCode").jsonPrimitive.content.toInt())
            assertEquals(true, body.getValue("maintenance").jsonPrimitive.content.toBoolean())
        } finally {
            FixtureNetwork.setBelowMinimum(context, false)
            FixtureNetwork.setMaintenance(context, false)
            FixtureNetwork.setNewerAvailable(context, false)
        }
    }

    @Test
    fun `the latency knob holds every answer for that long`() = runBlocking {
        FixtureNetwork.latencyOverride = 300

        val started = System.nanoTime()
        val response = client.get("https://dev.example.com/config")
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue("answered after $elapsedMs ms, under the 300 ms knob", elapsedMs >= 300)
    }

    @Test
    fun `the failure-rate knob fails that share, from the same seed every run`() = runBlocking {
        FixtureNetwork.failureRateOverride = 50

        val first = List(20) { client.get("https://dev.example.com/config").status }
        FixtureNetwork.resetFailures()
        val second = List(20) { client.get("https://dev.example.com/config").status }

        // Some fail and some do not — a rate, not a switch — and the run repeats from the seed.
        assertTrue(HttpStatusCode.ServiceUnavailable in first)
        assertTrue(HttpStatusCode.OK in first)
        assertEquals(first, second)
    }

    @Test
    fun `a failure rate of zero never fails and a hundred always does`() = runBlocking {
        FixtureNetwork.failureRateOverride = 0
        assertEquals(HttpStatusCode.OK, client.get("https://dev.example.com/config").status)

        FixtureNetwork.failureRateOverride = 100
        assertEquals(HttpStatusCode.ServiceUnavailable, client.get("https://dev.example.com/config").status)
    }

    @Test
    fun `a refresh is answered with a new pair`() = runBlocking {
        val response = refresh()

        assertEquals(HttpStatusCode.OK, response.status)
        val pair = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(pair.getValue("accessToken").jsonPrimitive.content.isNotBlank())
        assertTrue(pair.getValue("refreshToken").jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun `the armed expiry is a 401 and a refresh refused, once — so the session really ends`() = runBlocking {
        FixtureNetwork.sessionExpiryArmed = true

        assertEquals(HttpStatusCode.Unauthorized, client.get("https://dev.example.com/config").status)
        assertFalse(FixtureNetwork.sessionExpiryArmed)
        assertEquals(HttpStatusCode.Unauthorized, refresh().status)

        // Spent: the sign-in that follows is neither refused nor unable to refresh.
        assertEquals(HttpStatusCode.OK, refresh().status)
    }

    private suspend fun refresh() = client.post("https://dev.example.com/auth/refresh") {
        setBody(TextContent("""{"refreshToken":"refresh-1"}""", ContentType.Application.Json))
    }

    @Test
    fun `the failing switch breaks every route`() = runBlocking {
        FixtureNetwork.failingOverride = true

        assertEquals(HttpStatusCode.ServiceUnavailable, client.get("$APP/config").status)
        assertEquals(HttpStatusCode.ServiceUnavailable, client.get("$APP/messages").status)
    }

    @Test
    fun `a route the fixtures do not have is a 404`() = runBlocking {
        assertEquals(HttpStatusCode.NotFound, client.get("$APP/nothing-here").status)
    }

    @Test
    fun `with the switch on, a request leaves the process and its answer comes back`() = runBlocking {
        FixtureNetwork.realApiOverride = true

        val response = client.get("$APP/anything")

        assertEquals(listOf("$APP/anything"), forwarded)
        assertEquals(REAL_BODY, response.bodyAsText())
    }

    private companion object {

        const val APP = "https://dev.example.com"
        const val REAL_BODY = """{"real":true}"""
    }
}
