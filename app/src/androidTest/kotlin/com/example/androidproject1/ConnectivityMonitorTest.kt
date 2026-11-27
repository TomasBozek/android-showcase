package com.example.androidproject1

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidproject1.service.connectivity.AndroidConnectivityMonitor
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * The platform's own connectivity, not the `dev` flavor's switch: the emulator has a default
 * network, so the monitor's callback reports one.
 */
@RunWith(AndroidJUnit4::class)
class ConnectivityMonitorTest {

    @Test
    fun anEmulatorWithANetworkIsOnline() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val monitor = AndroidConnectivityMonitor(context, GlobalContext.get().get<DispatcherProvider>())

        val online = withTimeout(TIMEOUT_MS) { monitor.online.first { it } }

        assertTrue(online)
    }

    private companion object {

        const val TIMEOUT_MS = 10_000L
    }
}
