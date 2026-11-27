package com.example.androidproject1.service.files

import com.example.androidproject1.service.network.HttpClientFactory
import com.example.androidproject1.service.network.NetworkConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The download against Ktor's `MockEngine`: what it writes, what it reports and what a retry asks for. */
class DownloaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val body = ByteArray(300) { (it % 251).toByte() }
    private val ranges = mutableListOf<String?>()

    private fun downloader(engine: MockEngine) = KtorDownloader(
        HttpClientFactory.create(
            engine = engine,
            config = NetworkConfig(baseUrl = "https://example.test/", retries = 0),
        ),
    )

    private fun target() = File(folder.root, "movie.bin")

    @Test
    fun `a body is streamed to the file and the progress rises to Done`() = runTest {
        val engine = MockEngine {
            ranges += it.headers[HttpHeaders.Range]
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, body.size.toString()))
        }

        val progress = downloader(engine).download("https://example.test/movie", target()).toList()

        val running = progress.filterIsInstance<DownloadProgress.Running>()
        assertEquals(0L, running.first().bytes)
        assertEquals(body.size.toLong(), running.last().bytes)
        assertEquals(body.size.toLong(), running.last().total)
        assertEquals(running.map { it.bytes }.sorted(), running.map { it.bytes })
        assertEquals(DownloadProgress.Done(target()), progress.last())
        assertArrayEquals(body, target().readBytes())
        assertFalse("the partial file is gone once it is whole", File(folder.root, "movie.bin.part").exists())
        assertEquals(listOf<String?>(null), ranges)
    }

    @Test
    fun `a download cut off half-way keeps what arrived, and the retry asks for the rest`() = runTest {
        var attempt = 0
        val engine = MockEngine { request ->
            ranges += request.headers[HttpHeaders.Range]
            if (attempt++ == 0) {
                // The connection drops after HALF of the bytes the server promised.
                respond(
                    body.copyOf(HALF),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentLength, body.size.toString()),
                )
            } else {
                respond(
                    body.copyOfRange(HALF, body.size),
                    HttpStatusCode.PartialContent,
                    headersOf(HttpHeaders.ContentLength, (body.size - HALF).toString()),
                )
            }
        }
        val downloader = downloader(engine)

        val first = downloader.download("https://example.test/movie", target()).toList()

        assertTrue(first.last() is DownloadProgress.Failed)
        assertFalse(target().exists())
        assertEquals(HALF.toLong(), File(folder.root, "movie.bin.part").length())

        val second = downloader.download("https://example.test/movie", target()).toList()

        assertEquals(listOf<String?>(null, "bytes=$HALF-"), ranges)
        assertEquals(HALF.toLong(), second.filterIsInstance<DownloadProgress.Running>().first().bytes)
        assertEquals(DownloadProgress.Done(target()), second.last())
        assertArrayEquals(body, target().readBytes())
    }

    @Test
    fun `a server that ignores the range sends the whole file, which replaces the partial one`() = runTest {
        File(folder.root, "movie.bin.part").writeBytes(ByteArray(HALF) { 9 })
        val engine = MockEngine {
            ranges += it.headers[HttpHeaders.Range]
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, body.size.toString()))
        }

        val progress = downloader(engine).download("https://example.test/movie", target()).toList()

        assertEquals(listOf<String?>("bytes=$HALF-"), ranges)
        assertEquals(DownloadProgress.Done(target()), progress.last())
        assertArrayEquals(body, target().readBytes())
    }

    @Test
    fun `a partial file that already is the whole one is finished by the 416`() = runTest {
        File(folder.root, "movie.bin.part").writeBytes(body)
        val engine = MockEngine { respondError(HttpStatusCode.RequestedRangeNotSatisfiable) }

        val progress = downloader(engine).download("https://example.test/movie", target()).toList()

        assertEquals(listOf(DownloadProgress.Done(target())), progress)
        assertArrayEquals(body, target().readBytes())
    }

    @Test
    fun `a transport failure is a Failed, and nothing is left to look whole`() = runTest {
        val engine = MockEngine { throw java.io.IOException("no route") }

        val progress = downloader(engine).download("https://example.test/movie", target()).toList()

        assertTrue(progress.last() is DownloadProgress.Failed)
        assertFalse(target().exists())
    }

    @Test
    fun `a refusal is a Failed with the network's own error`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }

        val last = downloader(engine).download("https://example.test/movie", target()).toList().last()

        assertTrue(last is DownloadProgress.Failed)
        assertNull(target().takeIf { it.exists() })
    }

    private companion object {

        const val HALF = 100
    }
}
