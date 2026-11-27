package com.example.androidproject1.config

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.test.TestDispatchers
import com.example.androidproject1.service.network.HttpClientFactory
import com.example.androidproject1.service.network.NetworkConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The config fails open: what the server says is kept, what it said last applies while it
 * cannot be reached, and with nothing ever heard the app runs unheld.
 */
// A plain Application: the app's own starts Koin, once per test, and this needs none of it.
@Config(application = Application::class)
@RunWith(RobolectricTestRunner::class)
class DefaultAppConfigRepositoryTest {

    private val provider = DataStoreProvider(
        ApplicationProvider.getApplicationContext(),
        name = "config_test_${System.nanoTime()}",
    )

    private var answer: String? = """{"minVersionCode":5,"latestVersionCode":9,"maintenance":true}"""

    private fun repository() = DefaultAppConfigRepository(
        client = HttpClientFactory.create(
            engine = MockEngine { request ->
                val body = answer
                if (request.url.encodedPath.endsWith("config") && body != null) {
                    respond(body, HttpStatusCode.OK, headersOf("Content-Type", ContentType.Application.Json.toString()))
                } else {
                    respondError(HttpStatusCode.ServiceUnavailable)
                }
            },
            config = NetworkConfig(baseUrl = "https://fixtures.test/", retries = 0),
        ),
        dataStoreProvider = provider,
        dispatcherProvider = TestDispatchers,
    )

    @Test
    fun `the server's answer is the config`() = runTest {
        val repository = repository()

        repository.refresh()

        assertEquals(AppConfig(minVersionCode = 5, latestVersionCode = 9, maintenance = true), repository.config.value)
    }

    @Test
    fun `offline, the last answer applies — in a later process too`() = runTest {
        repository().refresh()
        answer = null

        val later = repository()
        later.refresh()

        assertEquals(AppConfig(minVersionCode = 5, latestVersionCode = 9, maintenance = true), later.config.value)
    }

    @Test
    fun `a dismissed update is remembered by version`() = runTest {
        repository().dismissUpdate(9)

        assertEquals(9, repository().dismissedUpdate())
    }

    @Test
    fun `offline with nothing ever heard, there is no config and so no hold`() = runTest {
        answer = null
        val repository = repository()

        repository.refresh()

        assertNull(repository.config.value)
    }
}
