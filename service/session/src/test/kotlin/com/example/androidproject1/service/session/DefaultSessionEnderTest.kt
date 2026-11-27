package com.example.androidproject1.service.session

import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.network.EndReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultSessionEnderTest {

    @Test
    fun `ending the session tells the owner once, however many requests found out`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val expiry = DefaultSessionExpiry()
        val ender = DefaultSessionEnder(expiry, FakeLogger())
        var heard = 0
        val job = launch { expiry.signals.first { heard++ >= 0 } }

        ender.end(EndReason.Unauthorized)
        ender.end(EndReason.Unauthorized)
        job.join()

        assertEquals(1, heard)
    }
}
