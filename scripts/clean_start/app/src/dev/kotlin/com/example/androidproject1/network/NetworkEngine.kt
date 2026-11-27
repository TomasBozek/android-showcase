package com.example.androidproject1.network

import android.content.Context
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.network.ConnectivityMonitor
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.random.Random

/**
 * The `dev` flavor talks to fixtures, not a server.
 *
 * A `MockEngine` rather than a bundled server: it needs nothing running, CI gets the same
 * behaviour as a laptop, and every answer is reviewable here. What it cannot do is go offline on
 * its own — the engine is in-process — which is what [FixtureNetwork.failing] is for: flip it and
 * every request fails, so the cache-then-network path can be seen by hand.
 *
 * It answers the app's own routes — the startup config, the server's messages and
 * the token refresh — and `404` for anything else. A feature that talks to a server adds its
 * routes to the `when` below, with its fixture JSON in `res/raw`.
 *
 * @param cacheSizeBytes the real engine's cache, for the requests that leave the process; a
 * fixture is never cached.
 */
fun networkEngine(context: Context, cacheSizeBytes: Long): HttpClientEngine = networkEngine(
    context = context,
    realClient = lazy {
        HttpClient(
            cachedOkHttpEngine(
                cacheDirectory = File(context.cacheDir, HTTP_CACHE_DIRECTORY),
                cacheSizeBytes = cacheSizeBytes,
            ),
        )
    },
)

/**
 * The engine with its real half injected, for the test: [realClient] is what every request goes to
 * while [FixtureNetwork.realApi] is set — and is never created otherwise, so a fixtures-only build
 * opens no cache directory.
 */
internal fun networkEngine(
    context: Context,
    realClient: Lazy<HttpClient>,
): HttpClientEngine {
    FixtureNetwork.attach(context)

    return MockEngine { request ->
        // The two knobs a fixture answers with before it answers at all: every answer
        // waits `latencyMs`, and one in `failureRate` percent is a 503, drawn from a generator
        // seeded once per process so a run repeats.
        delay(FixtureNetwork.latencyMs.toLong())
        // The session ends on the server: one `401`, then the switch is off again, and the
        // refresh the client makes next is refused as well.
        if (FixtureNetwork.sessionExpiryArmed) {
            FixtureNetwork.sessionExpiryArmed = false
            FixtureNetwork.refreshRefused = true
            return@MockEngine respondError(HttpStatusCode.Unauthorized)
        }
        if (FixtureNetwork.failing || FixtureNetwork.drawFailure()) {
            return@MockEngine respondError(HttpStatusCode.ServiceUnavailable)
        }
        // With the switch on, every request leaves the process.
        if (FixtureNetwork.realApi) return@MockEngine forward(realClient.value, request)

        val route = request.url.encodedPath.trimEnd('/')
        // The refresh: any refresh token is good for a new pair, unless the debug menu's
        // expiry was armed, which spends it. The pair is made up; nothing checks it.
        if (request.method == HttpMethod.Post && route.endsWith("auth/refresh")) {
            return@MockEngine if (FixtureNetwork.refreshRefused) {
                FixtureNetwork.refreshRefused = false
                respondError(HttpStatusCode.Unauthorized)
            } else {
                respondJson(
                    "{\"accessToken\":\"dev-access-${System.nanoTime()}\",\"refreshToken\":\"dev-refresh-${System.nanoTime()}\"}",
                )
            }
        }

        val body = when (route.substringAfterLast('/')) {
            // No message from the server until there is one to show.
            "messages" -> "[]"
            // The startup config, from the dev menu's three switches: nothing held by default.
            "config" -> FixtureNetwork.configJson()

            else -> return@MockEngine respondError(HttpStatusCode.NotFound)
        }
        respondJson(body)
    }
}

/** One request through the real client, answered as if the mock had: status, headers and body. */
private suspend fun MockRequestHandleScope.forward(client: HttpClient, request: HttpRequestData): HttpResponseData {
    val response = client.request {
        url(request.url)
        method = request.method
        headers.appendAll(request.headers)
        setBody(request.body)
    }
    return respond(content = response.bodyAsChannel(), status = response.status, headers = response.headers)
}

private fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode = HttpStatusCode.OK) = respond(
    content = body,
    status = status,
    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
)

/**
 * The fixture engine is in-process, so airplane mode means nothing to it and the one "offline"
 * `dev` has is its own switch: the banner and the retry policy follow [FixtureNetwork].
 *
 * @param dispatchers unused — nothing here is observed from the platform; the parameter exists so
 * the one call site in `ApplicationModule` compiles against every flavor's `connectivityMonitor`.
 */
@Suppress("UNUSED_PARAMETER")
fun connectivityMonitor(context: Context, dispatchers: DispatcherProvider): ConnectivityMonitor = FixtureNetwork

/**
 * Whether the fixture engine is pretending the server is down.
 *
 * Backed by the presence of a file, not just a flag, so it can be flipped from outside a running
 * process — the engine is in-process, so there is no airplane mode to reach for:
 *
 * ```bash
 * adb shell run-as com.example.androidproject1.dev touch files/fail_network   # break it
 * adb shell run-as com.example.androidproject1.dev rm files/fail_network      # fix it
 * ```
 *
 * The debug menu's offline switch writes the same file, so flipping it there and touching it over
 * adb are the same thing — except to [online], which follows the switch and the launch: a marker
 * touched over adb is seen by the engine at once and by the banner at the next flip or start.
 */
object FixtureNetwork : ConnectivityMonitor {

    private const val MARKER = "fail_network"
    private const val REAL_API_MARKER = "real_api"
    private const val LATENCY_MARKER = "latency_ms"
    private const val FAILURE_RATE_MARKER = "failure_rate"
    private const val BELOW_MINIMUM_MARKER = "config_below_minimum"
    private const val MAINTENANCE_MARKER = "config_maintenance"
    private const val NEWER_AVAILABLE_MARKER = "config_newer_available"
    private const val FAST_ORDER_CLOCK_MARKER = "fast_order_clock"
    private const val PRETEND_GOOGLE_PAY_MARKER = "pretend_google_pay"

    /** One seed, so which answers fail is the same on every run of a process. */
    private const val FAILURE_SEED = 20_260_922

    private var filesDir: java.io.File? = null

    /**
     * The next answer is a `401`, once. In memory rather than a marker file: it is a
     * one-shot, and a process death in between should not fire it into the next launch.
     */
    @Volatile
    var sessionExpiryArmed: Boolean = false

    /** Set by the armed `401` above: the next refresh is the one that comes back a `401`. */
    @Volatile
    var refreshRefused: Boolean = false

    /** Set from a var too, so a test can set it without touching the filesystem. */
    @Volatile
    var latencyOverride: Int? = null

    /**
     * How long every fixture answer waits, in milliseconds. The marker holds the number:
     *
     * ```bash
     * adb shell run-as com.example.androidproject1.dev sh -c 'echo 750 > files/latency_ms'
     * ```
     */
    val latencyMs: Int
        get() = latencyOverride ?: readNumber(LATENCY_MARKER)

    fun setLatencyMs(context: Context, latencyMs: Int) = writeNumber(context, LATENCY_MARKER, latencyMs)

    /** Set from a var too, so a test can set it without touching the filesystem. */
    @Volatile
    var failureRateOverride: Int? = null

    /**
     * The share of fixture answers that are a 503, in percent — `echo 50 >
     * files/failure_rate` over adb, the way [latencyMs] is set. Drawn per request from a
     * generator seeded once per process, so a run is reproducible; [resetFailures] restarts it.
     */
    val failureRate: Int
        get() = failureRateOverride ?: readNumber(FAILURE_RATE_MARKER)

    fun setFailureRate(context: Context, failureRate: Int) =
        writeNumber(context, FAILURE_RATE_MARKER, failureRate.coerceIn(0, 100))

    private var failures = Random(FAILURE_SEED)

    /** Whether this answer is one of the [failureRate] percent that fail. */
    internal fun drawFailure(): Boolean {
        val rate = failureRate
        return rate > 0 && failures.nextInt(100) < rate
    }

    /** Restarts the seeded draw, so a test sees the same sequence a fresh process would. */
    internal fun resetFailures() {
        failures = Random(FAILURE_SEED)
    }

    private fun readNumber(marker: String): Int =
        filesDir?.let { java.io.File(it, marker) }?.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 0

    private fun writeNumber(context: Context, marker: String, value: Int) {
        val file = java.io.File(context.filesDir, marker)
        if (value <= 0) file.delete() else file.writeText(value.toString())
    }

    /**
     * The startup config's three answers, each a marker so a relaunch — the only time the
     * config is asked for — sees the switch the tester just flipped: this build below the
     * server's minimum, the server down for maintenance, a newer build out.
     */
    val belowMinimum: Boolean get() = hasMarker(BELOW_MINIMUM_MARKER)

    fun setBelowMinimum(context: Context, on: Boolean) = setMarker(context, BELOW_MINIMUM_MARKER, on)

    val maintenance: Boolean get() = hasMarker(MAINTENANCE_MARKER)

    fun setMaintenance(context: Context, on: Boolean) = setMarker(context, MAINTENANCE_MARKER, on)

    val newerAvailable: Boolean get() = hasMarker(NEWER_AVAILABLE_MARKER)

    fun setNewerAvailable(context: Context, on: Boolean) = setMarker(context, NEWER_AVAILABLE_MARKER, on)

    /**
     * Whether the fixture's orders move thirty times over: packed in seconds rather than
     * minutes, so a timeline can be watched filling in. Read per request, like every other marker.
     */
    val fastOrderClock: Boolean get() = hasMarker(FAST_ORDER_CLOCK_MARKER)

    /** Google Pay pretended to be ready, for a device with no wallet; read live, a marker on disk. */
    val pretendGooglePay: Boolean get() = hasMarker(PRETEND_GOOGLE_PAY_MARKER)

    fun setPretendGooglePay(context: Context, on: Boolean) = setMarker(context, PRETEND_GOOGLE_PAY_MARKER, on)

    fun setFastOrderClock(context: Context, on: Boolean) = setMarker(context, FAST_ORDER_CLOCK_MARKER, on)

    /** `GET config`, from the three switches: `Int.MAX_VALUE` is above any build this fixture serves. */
    internal fun configJson(): String =
        """{"minVersionCode":${if (belowMinimum) Int.MAX_VALUE else 0},""" +
            """"latestVersionCode":${if (newerAvailable) Int.MAX_VALUE else 0},"maintenance":$maintenance}"""

    private fun hasMarker(marker: String): Boolean = filesDir?.let { java.io.File(it, marker).exists() } == true

    private fun setMarker(context: Context, marker: String, on: Boolean) {
        val file = java.io.File(context.filesDir, marker)
        if (on) file.createNewFile() else file.delete()
    }

    /** Set from a var too, so a test can flip it without touching the filesystem. */
    @Volatile
    var realApiOverride: Boolean = false

    /** Whether requests to the forwarded hosts leave the process. Read per request, like [failing]. */
    val realApi: Boolean
        get() = realApiOverride || filesDir?.let { java.io.File(it, REAL_API_MARKER).exists() } == true

    /** Writes the marker, so the choice survives the process it was made in. */
    fun setRealApi(context: Context, realApi: Boolean) {
        val marker = java.io.File(context.filesDir, REAL_API_MARKER)
        if (realApi) marker.createNewFile() else marker.delete()
    }

    /** Set from a var too, so a test can flip it without touching the filesystem. */
    @Volatile
    var failingOverride: Boolean = false
        set(value) {
            field = value
            publish()
        }

    val failing: Boolean
        get() = failingOverride || filesDir?.let { java.io.File(it, MARKER).exists() } == true

    private val onlineState = MutableStateFlow(true)

    /** The inverse of [failing], as the port the banner and the retry policy read. */
    override val online: StateFlow<Boolean> = onlineState.asStateFlow()

    internal fun attach(context: Context) {
        filesDir = context.filesDir
        publish()
    }

    /**
     * Writes the marker, so the choice survives the process it was made in — which is what a
     * tester expects of a switch that says "the server is down".
     */
    fun setFailing(context: Context, failing: Boolean) {
        val marker = java.io.File(context.filesDir, MARKER)
        if (failing) marker.createNewFile() else marker.delete()
        publish()
    }

    private fun publish() {
        onlineState.value = !failing
    }
}
