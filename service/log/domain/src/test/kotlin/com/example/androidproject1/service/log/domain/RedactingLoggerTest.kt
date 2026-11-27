package com.example.androidproject1.service.log.domain

import com.example.androidproject1.service.log.domain.test.FakeLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactingLoggerTest {

    private val fake = FakeLogger()
    private val logger = RedactingLogger(fake)

    private val logged: String get() = fake.entries.single().message

    @Test
    fun `the fixture profile's address is masked`() {
        // What `Profile.toString()` gives for the dev fixture's profile: a data class logged whole.
        logger.d { "Saved Profile(name=Ada Lovelace, email=ada.lovelace@example.com, avatarUri=null)" }

        assertFalse(logged.contains("ada.lovelace@example.com"))
        assertTrue(logged.contains("email=<email>"))
    }

    @Test
    fun `a bearer token is masked and the scheme kept`() {
        logger.w { "-> Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MiJ9.c2lnbmF0dXJl" }

        assertEquals("-> Authorization: Bearer <token>", logged)
    }

    @Test
    fun `a token in a JSON body is masked`() {
        logger.d { """BODY {"access_token":"abc.def-123","refresh_token":"r-456","expires_in":3600}""" }

        assertEquals("""BODY {"access_token":"<token>","refresh_token":"<token>","expires_in":3600}""", logged)
    }

    @Test
    fun `a card number is masked, spaced or not, and a long id is not`() {
        logger.e { "Paying with 4242 4242 4242 4242 and 5555555555554444 for order 1696159200000" }

        assertFalse(logged.contains("4242"))
        assertFalse(logged.contains("5555555555554444"))
        // Thirteen digits that fail the Luhn check are an id or a timestamp, not a card.
        assertTrue(logged.contains("order 1696159200000"))
        assertEquals("Paying with <card> and <card> for order 1696159200000", logged)
    }

    @Test
    fun `the level and the throwable pass through untouched`() {
        val boom = IllegalStateException("boom")

        logger.withTag("Cart").e(boom) { "failed for ada@example.com" }

        val entry = fake.entries.single()
        assertEquals(FakeLogger.Level.ERROR, entry.level)
        assertSame(boom, entry.throwable)
        assertEquals("failed for <email>", entry.message)
    }

    @Test
    fun `a line with nothing personal is left as it is`() {
        logger.d { "GET https://api.example.com/v1/products?page=2 -> 200 in 143 ms" }

        assertEquals("GET https://api.example.com/v1/products?page=2 -> 200 in 143 ms", logged)
    }
}
