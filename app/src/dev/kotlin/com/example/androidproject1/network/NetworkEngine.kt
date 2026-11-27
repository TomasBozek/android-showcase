package com.example.androidproject1.network

import android.content.Context
import com.example.androidproject1.R
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
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * The `dev` flavor talks to fixtures, not a server.
 *
 * A `MockEngine` rather than a bundled server: it needs nothing running, CI gets the same
 * behaviour as a laptop, and the fixtures are reviewable JSON in `res/raw`. What it cannot do is
 * go offline on its own — the engine is in-process — which is what [FixtureNetwork.failing] is
 * for: flip it and every request fails, so the cache-then-network path can be seen by hand.
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
 * The engine with its real half injected, for the test: [realClient] is what a request to a
 * forwarded host goes to while [FixtureNetwork.realApi] is set — and is never created otherwise, so a
 * fixtures-only build opens no cache directory. A bare client over the real engine, because the
 * client above this engine already retries, logs and parses: the request arrives here finished.
 */
internal fun networkEngine(
    context: Context,
    realClient: Lazy<HttpClient>,
): HttpClientEngine {
    val categories = context.readRaw(R.raw.fixture_catalog_categories)
    val products = context.readRaw(R.raw.fixture_catalog_products)
    val messages = context.readRaw(R.raw.fixture_messages)
    val checkout = CheckoutFixtures(
        products = products,
        delivery = context.readRaw(R.raw.fixture_checkout_delivery),
        orderCount = java.io.File(context.filesDir, ORDER_COUNT_MARKER),
    )
    val shows = TvMazeFixtures(context)
    val weather = OpenMeteoFixtures(context)
    FixtureNetwork.attach(context)

    return MockEngine { request ->
        // The two knobs a fixture answers with before it answers at all: every answer
        // waits `latencyMs`, so a loading state, a retry and a stale badge can be watched on a
        // device; and one in `failureRate` percent is a 503, drawn from a generator seeded once
        // per process so a run repeats — the same fourth request fails every time.
        delay(FixtureNetwork.latencyMs.toLong())
        // The session ends on the server: one `401`, then the switch is off again, so the
        // sign-in that follows is not refused too. The refresh the client makes next is refused as
        // well — a refresh that succeeded would just carry the session on, and the switch would
        // end nothing.
        if (FixtureNetwork.sessionExpiryArmed) {
            FixtureNetwork.sessionExpiryArmed = false
            FixtureNetwork.refreshRefused = true
            return@MockEngine respondError(HttpStatusCode.Unauthorized)
        }
        if (FixtureNetwork.failing || FixtureNetwork.drawFailure()) {
            // 503 rather than a thrown IOException: it exercises the status table as well as the
            // stale-cache path, and it is what a real server does when it is having a bad day.
            return@MockEngine respondError(HttpStatusCode.ServiceUnavailable)
        }

        // The host first: TVmaze and Open-Meteo are servers with paths of their own, and
        // `shows/1` and `product?id=` must not be told apart by their last segment alone. With
        // the switch on, a request to a forwarded host leaves the process; none needs a key.
        val host = request.url.host
        if (host in FORWARDED_HOSTS) {
            return@MockEngine if (FixtureNetwork.realApi) {
                // A hundred megabytes are streamed through, not held: `forward` reads a body whole.
                if (host == SAMPLE_FILE_HOST) stream(realClient.value, request) else forward(realClient.value, request)
            } else {
                when (host) {
                    TVMAZE_HOST -> shows.handle(this, request.url)
                    SAMPLE_FILE_HOST -> SampleFileFixture.handle(this, request)
                    else -> weather.handle(this, request.url)
                }
            }
        }

        // A reset link is mailed for any address — a real server does not say which are
        // registered — and a second ask inside the screen's countdown is refused, so the resend
        // timer can be seen doing its job.
        if (request.url.encodedPath.trimEnd('/').endsWith("auth/password/reset")) {
            return@MockEngine if (request.method == HttpMethod.Post) {
                val now = System.currentTimeMillis()
                val tooSoon = now - lastPasswordReset < PASSWORD_RESET_WINDOW_MS
                if (!tooSoon) lastPasswordReset = now
                if (tooSoon) {
                    respondError(
                        HttpStatusCode.TooManyRequests,
                    )
                } else {
                    respond(content = "", status = HttpStatusCode.Accepted)
                }
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }

        // The link's new password: the token `fixture` is the one this fixture ever
        // issues, so it is accepted and any other is refused as a link that expired.
        if (request.url.encodedPath.trimEnd('/').endsWith("auth/password") && request.method == HttpMethod.Post) {
            val token = (request.body as? TextContent)?.text?.let {
                Regex("\"token\"\\s*:\\s*\"([^\"]*)\"").find(it)?.groupValues?.get(1)
            }
            return@MockEngine if (token == FIXTURE_RESET_TOKEN) {
                respond(content = "", status = HttpStatusCode.OK)
            } else {
                respondError(HttpStatusCode.BadRequest)
            }
        }

        // The refresh: any refresh token is good for a new pair, unless the debug menu's
        // expiry was armed, which spends it. The pair is made up; nothing checks it.
        if (request.method == HttpMethod.Post && request.url.encodedPath.trimEnd('/').endsWith("auth/refresh")) {
            return@MockEngine if (FixtureNetwork.refreshRefused) {
                FixtureNetwork.refreshRefused = false
                respondError(HttpStatusCode.Unauthorized)
            } else {
                respondJson(
                    "{\"accessToken\":\"dev-access-${System.nanoTime()}\",\"refreshToken\":\"dev-refresh-${System.nanoTime()}\"}",
                )
            }
        }

        // Sign-up mails a code, which the fixture never sends: `000000` is the one it accepts,
        // and a resend is always taken.
        val route = request.url.encodedPath.trimEnd('/')
        if (request.method == HttpMethod.Post &&
            (route.endsWith("auth/signup") || route.endsWith("auth/verify/resend"))
        ) {
            return@MockEngine respond(content = "", status = HttpStatusCode.Accepted)
        }
        if (request.method == HttpMethod.Post && route.endsWith("auth/verify")) {
            val code = (request.body as? TextContent)?.text?.let {
                Regex("\"code\"\\s*:\\s*\"([^\"]*)\"").find(it)?.groupValues?.get(1)
            }
            return@MockEngine if (code == FIXTURE_VERIFICATION_CODE) {
                respond(content = "", status = HttpStatusCode.OK)
            } else {
                respondError(HttpStatusCode.BadRequest)
            }
        }

        // Inventory's outbox: a put is accepted and a delete is done — except a put for an
        // item whose name says "conflict", answered 409, so a refused write can be seen on a device
        // without a server that disagrees. The failing switch above reaches it like every route.
        if (request.url.encodedPath.contains("/inventory/items/")) {
            return@MockEngine when (request.method) {
                HttpMethod.Put -> {
                    val body = (request.body as? TextContent)?.text.orEmpty()
                    if (body.contains("conflict", ignoreCase = true)) {
                        respondError(HttpStatusCode.Conflict)
                    } else {
                        respond(
                            content = body,
                            status = HttpStatusCode.OK,
                            headers = headersOf("Content-Type", "application/json"),
                        )
                    }
                }
                HttpMethod.Delete -> respond(content = "", status = HttpStatusCode.NoContent)
                else -> respondError(HttpStatusCode.MethodNotAllowed)
            }
        }

        // The avatar's upload: taken whole, read a piece at a time so the progress on
        // Profile moves on a device instead of jumping to the end.
        if (route.endsWith("profile/avatar")) {
            return@MockEngine if (request.method == HttpMethod.Put) {
                drain(request.body)
                respond(content = "", status = HttpStatusCode.OK)
            } else {
                respondError(HttpStatusCode.MethodNotAllowed)
            }
        }

        val path = request.url.encodedPath.trimEnd('/').substringAfterLast('/')
        // The one route told apart by its method: the account is deleted, never read.
        if (path == "account") {
            return@MockEngine if (request.method == HttpMethod.Delete) {
                respond(content = "", status = HttpStatusCode.NoContent)
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }
        // Checkout's one write: priced here from the catalog, declined for the published
        // test cards, so both ends of the review step can be seen on a device.
        if (path == "orders" && request.method == HttpMethod.Post) {
            return@MockEngine checkout.placeOrder(this, (request.body as? TextContent)?.text.orEmpty())
        }
        if (path == "orders") return@MockEngine respondJson(checkout.orders())
        val body = when (path) {
            "categories" -> categories
            "delivery" -> checkout.delivery
            "promo" -> checkout.promo(request.url.parameters["code"])
                ?: return@MockEngine respondError(HttpStatusCode.NotFound)
            // One banner, live all of 2026 and 2027, whose action opens a product.
            "messages" -> messages
            // The startup config, from the dev menu's three switches: nothing held by default.
            "config" -> FixtureNetwork.configJson()
            "products" -> products.forCategory(request.url.parameters["categoryId"])
            "product" -> products.forProduct(request.url.parameters["id"])
                ?: return@MockEngine respondError(HttpStatusCode.NotFound)

            else -> return@MockEngine respondError(HttpStatusCode.NotFound)
        }
        respondJson(body)
    }
}

/**
 * The form two spellings of a place are compared in: NFD with the combining marks dropped, the
 * few Latin letters NFD leaves whole mapped by hand, and the case folded — so `plzen` finds
 * Plzeň, `zur` finds Zürich and `koben` finds København.
 *
 * `java.text.Normalizer` rather than a table of accented letters, and here rather than in the
 * feature: this is the fixture host standing in for a geocoder that folds, not a screen
 * formatting a number — the ban on `NumberFormat` and its neighbours is about roles a screen
 * should ask `Formats` for, and this is neither.
 */
private fun String.folded(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase()
        .map { WHOLE_LETTERS[it] ?: it.toString() }
        .joinToString("")

/** Unicode's combining-marks block: what NFD splits an accented letter into. */
private val COMBINING_MARKS = Regex("\\p{Mn}+")

/**
 * The letters NFD does not decompose, because they are letters in their own right rather than a
 * base with a mark on it — a Dane typing `koben` still means København. Short and closed on
 * purpose: it covers the alphabets this fixture's places are spelled in, and the real geocoder
 * is the one that has to know every language.
 */
private val WHOLE_LETTERS = mapOf(
    'ø' to "o",
    'æ' to "ae",
    'œ' to "oe",
    'ß' to "ss",
    'đ' to "d",
    'ł' to "l",
)

/** What Open-Meteo returns when a request names no `count`. */
private const val DEFAULT_COUNT = 10

private const val TVMAZE_HOST = "api.tvmaze.com"
private const val GEOCODING_HOST = "geocoding-api.open-meteo.com"
private const val FORECAST_HOST = "api.open-meteo.com"

/** Where the 100 MB sample download comes from: a public speed-test host, answered here unless the switch is on. */
internal const val SAMPLE_FILE_HOST = "speed.cloudflare.com"

/** The third-party hosts the switch sends out of the process; everything else is fixtures. */
private val FORWARDED_HOSTS = setOf(TVMAZE_HOST, GEOCODING_HOST, FORECAST_HOST, SAMPLE_FILE_HOST)

/**
 * What the fixture engine answers under the two Open-Meteo hosts: `v1/search?name=` from
 * forty places, matched by prefix the way the real geocoder does — folding diacritics, so `plzen`
 * finds Plzeň — capped at `count`, and `v1/forecast` from one
 * real answer — its `latitude`/`longitude` echoed from the query, and every `time` shifted so
 * the first hour is today at 00:00: a forecast dated the day the fixture was fetched is one the
 * screen would rightly show as stale.
 */
private class OpenMeteoFixtures(context: Context, private val today: () -> LocalDate = LocalDate::now) {

    private val places: List<JsonObject> = Json.parseToJsonElement(context.readRaw(R.raw.fixture_weather_geocoding))
        .jsonObject.getValue("results").jsonArray.map { it.jsonObject }

    private val forecast: JsonObject = Json.parseToJsonElement(
        context.readRaw(R.raw.fixture_weather_forecast),
    ).jsonObject

    fun handle(scope: MockRequestHandleScope, url: Url): HttpResponseData = with(scope) {
        val route = url.segments.filter { it.isNotEmpty() }
        when {
            url.host == GEOCODING_HOST && route == listOf("v1", "search") -> {
                val query = url.parameters["name"].orEmpty().trim()
                // What the real geocoder caps at; a fixture that answered more would hand the
                // screen a list no real answer could produce.
                val count = url.parameters["count"]?.toIntOrNull() ?: DEFAULT_COUNT
                val matches = if (query.isEmpty()) {
                    emptyList()
                } else {
                    val folded = query.folded()
                    places
                        .filter { it.getValue("name").jsonPrimitive.content.folded().startsWith(folded) }
                        .take(count)
                }
                // No `results` key at all for no match — which is what the real geocoder sends.
                respondJson(
                    if (matches.isEmpty()) "{}" else JsonObject(mapOf("results" to JsonArray(matches))).toString(),
                )
            }

            url.host == FORECAST_HOST && route == listOf("v1", "forecast") -> {
                val latitude = url.parameters["latitude"]?.toDoubleOrNull()
                val longitude = url.parameters["longitude"]?.toDoubleOrNull()
                if (latitude == null || longitude == null) {
                    respondJson(BAD_REQUEST, HttpStatusCode.BadRequest)
                } else {
                    respondJson(forecastFor(latitude, longitude))
                }
            }

            else -> respondJson(NOT_FOUND, HttpStatusCode.NotFound)
        }
    }

    private fun forecastFor(latitude: Double, longitude: Double): String {
        val fixtureDay = LocalDate.parse(
            forecast.getValue("daily").jsonObject.getValue("time").jsonArray.first().jsonPrimitive.content,
        )
        val shift = ChronoUnit.DAYS.between(fixtureDay, today())
        fun shifted(time: String): JsonPrimitive = JsonPrimitive(
            if (time.length > DATE_LENGTH) {
                LocalDateTime.parse(time).plusDays(shift).toString()
            } else {
                LocalDate.parse(time).plusDays(shift).toString()
            },
        )
        fun withTimes(section: JsonObject): JsonObject = JsonObject(
            section +
                ("time" to JsonArray(section.getValue("time").jsonArray.map { shifted(it.jsonPrimitive.content) })),
        )
        val current = forecast.getValue("current").jsonObject
        return JsonObject(
            forecast +
                mapOf(
                    "latitude" to JsonPrimitive(latitude),
                    "longitude" to JsonPrimitive(longitude),
                    "current" to
                        JsonObject(current + ("time" to shifted(current.getValue("time").jsonPrimitive.content))),
                    "hourly" to withTimes(forecast.getValue("hourly").jsonObject),
                    "daily" to withTimes(forecast.getValue("daily").jsonObject),
                ),
        ).toString()
    }

    private companion object {

        const val DATE_LENGTH = 10
        const val BAD_REQUEST = """{"error":true,"reason":"Latitude and longitude are required."}"""
        const val NOT_FOUND = """{"error":true,"reason":"Not found."}"""
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

/**
 * [forward], but the body is handed on as it arrives. The request runs inside a writer that
 * outlives this call, and the answer is made as soon as the real one's head is in.
 */
private suspend fun MockRequestHandleScope.stream(client: HttpClient, request: HttpRequestData): HttpResponseData {
    val head = CompletableDeferred<Pair<HttpStatusCode, Headers>>()
    val writer = CoroutineScope(Dispatchers.IO).writer(autoFlush = true) {
        try {
            client.prepareRequest {
                url(request.url)
                method = request.method
                headers.appendAll(request.headers)
            }.execute { response ->
                head.complete(response.status to response.headers)
                response.bodyAsChannel().copyTo(channel)
            }
        } catch (failure: Throwable) {
            head.completeExceptionally(failure)
            throw failure
        }
    }
    val (status, headers) = head.await()
    return respond(content = writer.channel, status = status, headers = headers)
}

/** Reads [body] to its end, [UPLOAD_CHUNK] bytes every [UPLOAD_PAUSE_MS] — a slow uplink, on a device. */
private suspend fun drain(body: OutgoingContent) = coroutineScope {
    val channel = when (body) {
        is OutgoingContent.ReadChannelContent -> body.readFrom()
        is OutgoingContent.WriteChannelContent -> writer { body.writeTo(channel) }.channel
        is OutgoingContent.ByteArrayContent -> ByteReadChannel(body.bytes())
        else -> ByteReadChannel.Empty
    }
    val buffer = ByteArray(UPLOAD_CHUNK)
    while (channel.readAvailable(buffer, 0, buffer.size) >= 0) delay(UPLOAD_PAUSE_MS)
}

private const val UPLOAD_CHUNK = 8 * 1024
private const val UPLOAD_PAUSE_MS = 250L

private fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode = HttpStatusCode.OK) = respond(
    content = body,
    status = status,
    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
)

/**
 * What the fixture engine answers under [TVMAZE_HOST]: `shows?page=n` — numbered from 0 —
 * from a hundred shows in TVmaze's own shape, and `shows/<id>` as the same object, because TVmaze's
 * list and detail answer the same one. A page past the last answers `404`, as the real host does,
 * so the client's end-of-list handling is exercised on the same status.
 *
 * The rows are real shows — ids, names, genres, dates, ratings, image URLs — with a one-line
 * summary of our own, in HTML, so the overview's tag stripping is exercised too.
 */
private class TvMazeFixtures(context: Context) {

    private val rows: List<JsonObject> = Json.parseToJsonElement(context.readRaw(R.raw.fixture_movies_shows))
        .jsonArray.map { it.jsonObject }

    /** Four pages of twenty-five; TVmaze's are 250, which is more than a fixture needs. */
    private val pages: List<List<JsonObject>> = rows.chunked(PAGE_SIZE)

    private val byId: Map<String, JsonObject> = rows.associateBy { it.getValue("id").jsonPrimitive.content }

    fun handle(scope: MockRequestHandleScope, url: Url): HttpResponseData = with(scope) {
        val route = url.segments.filter { it.isNotEmpty() }
        when {
            route == listOf("shows") -> {
                val page = url.parameters["page"]?.toIntOrNull() ?: 0
                pages.getOrNull(page)?.let { respondJson(JsonArray(it).toString()) }
                    ?: respondJson(NOT_FOUND, HttpStatusCode.NotFound)
            }

            route.size == 2 && route[0] == "shows" -> byId[route[1]]?.let { respondJson(it.toString()) }
                ?: respondJson(NOT_FOUND, HttpStatusCode.NotFound)

            else -> respondJson(NOT_FOUND, HttpStatusCode.NotFound)
        }
    }

    private companion object {

        const val PAGE_SIZE = 25
        const val NOT_FOUND = """{"name":"Not Found","message":"","code":0,"status":404}"""
    }
}

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

private fun Context.readRaw(id: Int): String =
    resources.openRawResource(id).bufferedReader().use { it.readText() }

/**
 * Filters the product fixture the way the endpoint would.
 *
 * Parsed rather than string-sliced: a fixture that got its own JSON subtly wrong would fail the
 * app for a reason that has nothing to do with the app.
 */
private fun String.forCategory(categoryId: String?): String {
    if (categoryId == null) return this
    val kept = Json.parseToJsonElement(this).jsonArray.filter {
        it.jsonObject["categoryId"]?.jsonPrimitive?.content == categoryId
    }
    return JsonArray(kept).toString()
}

/**
 * One product's own JSON object, by id — `null` if the fixture has no such product, which is a
 * 404 to the caller. The full fixture list is searched regardless of what the local table holds:
 * the "server" knows about every product whether or not this session has browsed to it.
 */
private fun String.forProduct(productId: String?): String? {
    val found = Json.parseToJsonElement(this).jsonArray.firstOrNull {
        it.jsonObject["id"]?.jsonPrimitive?.content == productId
    } ?: return null
    return found.toString()
}

/** The file [CheckoutFixtures] counts placed orders in, beside the switches' markers. */
private const val ORDER_COUNT_MARKER = "orders_placed"

/**
 * What the fixture engine answers for checkout and orders: `GET delivery` from its
 * fixture, `GET promo` for the one code it honours, `POST orders` priced from the product fixture —
 * the lines by id and count, less the promo, plus the delivery — with the published decline cards
 * answering `402`, the way a payment processor's test mode does; the card arrives as a token,
 * `tok_<number>`. `GET orders` lists what was placed in this process, and two older orders so a
 * fresh install has a list, each moved Placed → Packed → Shipped → Delivered on [ORDER_CLOCK] —
 * minutes, or seconds with the dev menu's fast order clock on.
 */
private class CheckoutFixtures(
    products: String,
    val delivery: String,
    private val orderCount: java.io.File,
    private val now: () -> Instant = Instant::now,
) {

    private val catalog: Map<String, JsonObject> =
        Json.parseToJsonElement(products).jsonArray.associate { it.jsonObject.string("id") to it.jsonObject }

    private val deliveries: Map<String, JsonObject> =
        Json.parseToJsonElement(delivery).jsonArray.associate { it.jsonObject.string("id") to it.jsonObject }

    /** What `POST orders` took in this process, oldest first; a process death forgets it, as a device's cache would not. */
    private val placed = mutableListOf<PlacedFixture>()

    fun promo(code: String?): String? {
        if (!code.equals(PROMO_CODE, ignoreCase = true)) return null
        return """{"code":"$PROMO_CODE","percentOff":$PROMO_PERCENT}"""
    }

    fun placeOrder(scope: MockRequestHandleScope, body: String): HttpResponseData = with(scope) {
        val order = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return respondJson(BAD_ORDER, HttpStatusCode.BadRequest)
        DECLINES[order["cardToken"]?.jsonPrimitive?.content]?.let { reason ->
            return respondJson("""{"reason":"$reason"}""", HttpStatusCode.PaymentRequired)
        }
        val lines = order["lines"]?.jsonArray.orEmpty().map { line ->
            val product = catalog[line.jsonObject.string("productId")]
                ?: return respondJson(BAD_ORDER, HttpStatusCode.BadRequest)
            FixtureLine(
                product.string("id"),
                product.string("name"),
                product.long("price"),
                line.jsonObject.long("quantity"),
            )
        }
        val travel = deliveries[order["deliveryId"]?.jsonPrimitive?.content]
            ?: return respondJson(BAD_ORDER, HttpStatusCode.BadRequest)
        val subtotal = lines.sumOf { it.price * it.quantity }
        val promoCode = order["promoCode"]?.jsonPrimitive?.content
        val discount = if (promo(promoCode) == null) 0 else subtotal * PROMO_PERCENT / 100
        val id = "A" + (ORDER_ID_BASE + nextOrderNumber()).toString(ORDER_ID_RADIX).uppercase()
        val fixture = PlacedFixture(
            id = id,
            placedAt = now().truncatedTo(ChronoUnit.SECONDS),
            lines = lines,
            address = order["address"]?.jsonObject ?: return respondJson(BAD_ORDER, HttpStatusCode.BadRequest),
            deliveryName = travel.string("name"),
            deliveryPrice = travel.long("price"),
            discount = discount,
            total = subtotal - discount + travel.long("price"),
        )
        placed += fixture
        respondJson(
            """{"id":"$id","total":${fixture.total},"discount":$discount,"placedAt":"${fixture.placedAt}"}""",
            HttpStatusCode.Created,
        )
    }

    /**
     * How many orders this install has placed, counting this one. On disk, not [placed]'s size: the
     * device keeps orders by id and ignores one it already holds, so a count that started again
     * with the process gave the first order after a restart the id of one already kept — and the
     * new order was lost. Cleared with the app's data, the way the orders it numbers are.
     */
    private fun nextOrderNumber(): Long {
        val number = (orderCount.takeIf { it.exists() }?.readText()?.trim()?.toLongOrNull() ?: 0L) + 1
        orderCount.writeText(number.toString())
        return number
    }

    /** `GET orders`: every order this fixture knows, each with the history its clock has reached. */
    fun orders(): String = JsonArray((seeded() + placed).map { it.toJson(now()) }).toString()

    /** Two older orders, dated back from now so they always read as last month's. */
    private fun seeded(): List<PlacedFixture> {
        val coffee = catalog.getValue("coffee")
        val tea = catalog.getValue("tea")
        val address = JsonObject(
            mapOf(
                "name" to JsonPrimitive("Ada Lovelace"),
                "street" to JsonPrimitive("Na Příkopě 1"),
                "city" to JsonPrimitive("Praha"),
                "postalCode" to JsonPrimitive("110 00"),
                "country" to JsonPrimitive("CZ"),
                "phone" to JsonPrimitive("+420777123456"),
            ),
        )
        val today = now().truncatedTo(ChronoUnit.DAYS)
        return listOf(
            PlacedFixture(
                id = "Z0DLV1",
                placedAt = today.minus(SEEDED_DELIVERED_DAYS, ChronoUnit.DAYS),
                lines = listOf(FixtureLine("coffee", coffee.string("name"), coffee.long("price"), 3)),
                address = address,
                deliveryName = "Standard courier",
                deliveryPrice = STANDARD_PRICE,
                discount = 0,
                total = coffee.long("price") * 3 + STANDARD_PRICE,
            ),
            PlacedFixture(
                id = "Z0CNL1",
                placedAt = today.minus(SEEDED_CANCELLED_DAYS, ChronoUnit.DAYS),
                lines = listOf(FixtureLine("tea", tea.string("name"), tea.long("price"), 2)),
                address = address,
                deliveryName = "Pickup point",
                deliveryPrice = 0,
                discount = 0,
                total = tea.long("price") * 2,
                cancelled = true,
            ),
        )
    }

    private data class FixtureLine(val productId: String, val name: String, val price: Long, val quantity: Long)

    private data class PlacedFixture(
        val id: String,
        val placedAt: Instant,
        val lines: List<FixtureLine>,
        val address: JsonObject,
        val deliveryName: String,
        val deliveryPrice: Long,
        val discount: Long,
        val total: Long,
        val cancelled: Boolean = false,
    ) {

        fun toJson(now: Instant): JsonObject = JsonObject(
            mapOf(
                "id" to JsonPrimitive(id),
                "placedAt" to JsonPrimitive(placedAt.toString()),
                "lines" to JsonArray(
                    lines.map {
                        JsonObject(
                            mapOf(
                                "productId" to JsonPrimitive(it.productId),
                                "name" to JsonPrimitive(it.name),
                                "price" to JsonPrimitive(it.price),
                                "quantity" to JsonPrimitive(it.quantity),
                            ),
                        )
                    },
                ),
                "address" to address,
                "deliveryName" to JsonPrimitive(deliveryName),
                "deliveryPrice" to JsonPrimitive(deliveryPrice),
                "discount" to JsonPrimitive(discount),
                "total" to JsonPrimitive(total),
                "history" to JsonArray(history(now)),
            ),
        )

        private fun history(now: Instant): List<JsonObject> {
            val speed = if (FixtureNetwork.fastOrderClock) FAST_CLOCK else 1L
            val reached = if (cancelled) {
                listOf("placed" to placedAt, "cancelled" to placedAt.plus(CANCELLED_AFTER_MINUTES, ChronoUnit.MINUTES))
            } else {
                ORDER_CLOCK.map { (status, minutes) ->
                    status to
                        placedAt.plusSeconds(minutes * SECONDS_PER_MINUTE / speed)
                }
            }
            return reached.filter { (_, at) -> !at.isAfter(now) }.map { (status, at) ->
                JsonObject(mapOf("status" to JsonPrimitive(status), "at" to JsonPrimitive(at.toString())))
            }
        }
    }

    private companion object {

        const val PROMO_CODE = "WELCOME10"
        const val PROMO_PERCENT = 10
        const val ORDER_ID_BASE = 36L * 36 * 36 * 36
        const val ORDER_ID_RADIX = 36
        const val STANDARD_PRICE = 490L
        const val SEEDED_DELIVERED_DAYS = 12L
        const val SEEDED_CANCELLED_DAYS = 30L
        const val CANCELLED_AFTER_MINUTES = 20L
        const val SECONDS_PER_MINUTE = 60L

        /** The fast order clock runs thirty times over: two minutes to packed becomes four seconds. */
        const val FAST_CLOCK = 30L
        const val BAD_ORDER = """{"error":"An order needs its lines, a delivery and a card."}"""

        /** Stripe's published test cards: a generic decline and one for insufficient funds. */
        val DECLINES = mapOf(
            "tok_4000000000000002" to "card_declined",
            "tok_4000000000009995" to "insufficient_funds",
        )
    }
}

/** The minutes after it was placed that an order reaches each status, on the fixture's clock. */
private val ORDER_CLOCK = listOf("placed" to 0L, "packed" to 2L, "shipped" to 10L, "delivered" to 30L)

private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.long(key: String): Long = getValue(key).jsonPrimitive.content.toLong()

/** When the fixture last accepted a reset request; per process, like every other fixture knob. */
@Volatile
private var lastPasswordReset = 0L

/** The reset screen's resend countdown; a second request inside it answers `429`. */
private const val PASSWORD_RESET_WINDOW_MS = 30_000L

/** The only reset token the fixture issues: `<applicationId>://reset/fixture` is the link that works. */
private const val FIXTURE_RESET_TOKEN = "fixture"

/** The only sign-up code the fixture accepts. */
private const val FIXTURE_VERIFICATION_CODE = "000000"

/**
 * The 100 MB sample file: bytes made up as they are asked for, a quarter of a megabyte at a
 * time with a pause between, so the whole file takes about sixteen seconds and a progress bar,
 * a cancel and a resume can all be watched. It answers `Range: bytes=N-` with `206`, as the real
 * host does, so a download that was stopped carries on from what it had.
 */
internal object SampleFileFixture {

    const val TOTAL_BYTES = 100L * 1024 * 1024
    private const val CHUNK = 256 * 1024
    private const val PAUSE_MS = 40L

    fun handle(scope: MockRequestHandleScope, request: HttpRequestData): HttpResponseData = with(scope) {
        val total = request.url.parameters["bytes"]?.toLongOrNull()?.coerceIn(1, TOTAL_BYTES * 10) ?: TOTAL_BYTES
        val from = request.headers[HttpHeaders.Range]
            ?.removePrefix("bytes=")?.substringBefore('-')?.toLongOrNull() ?: 0L
        if (from >= total) {
            return respond(
                content = "",
                status = HttpStatusCode.RequestedRangeNotSatisfiable,
                headers = headersOf(HttpHeaders.ContentRange, "bytes */$total"),
            )
        }
        val remaining = total - from
        val body = CoroutineScope(Dispatchers.IO).writer(autoFlush = true) {
            val chunk = ByteArray(CHUNK) { (it % 251).toByte() }
            var left = remaining
            while (left > 0) {
                val size = minOf(left, CHUNK.toLong()).toInt()
                channel.writeFully(chunk, 0, size)
                left -= size
                delay(PAUSE_MS)
            }
        }
        val headers = buildMap {
            put(HttpHeaders.ContentLength, listOf(remaining.toString()))
            put(HttpHeaders.AcceptRanges, listOf("bytes"))
            if (from > 0) put(HttpHeaders.ContentRange, listOf("bytes $from-${total - 1}/$total"))
        }
        respond(
            content = body.channel,
            status = if (from > 0) HttpStatusCode.PartialContent else HttpStatusCode.OK,
            headers = Headers.build { headers.forEach { (k, v) -> appendAll(k, v) } },
        )
    }
}
