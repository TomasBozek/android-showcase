package com.example.androidproject1.service.session

import com.example.androidproject1.service.network.Tokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val ADA = Session(id = "1c2f", email = "ada@example.com", termsVersion = 1, tokens = null)
private val TOKENS = Tokens(accessToken = "access-1", refreshToken = "refresh-1")

/** The store's own field separator, spelled out so the malformed cases below are readable. */
private const val SEPARATOR = "\u001F"

/**
 * The session, against the real encrypted store rather than a fake `DataStore`.
 *
 * The three cases the class promises all read as signed out — an absent file, an undecryptable one
 * and one written in a format this build does not know — are only real if the file is real, so
 * every test here goes through AES-GCM and the disk.
 */
@RunWith(RobolectricTestRunner::class)
class EncryptedSessionStoreTest {

    private lateinit var store: TestSessionStore
    private lateinit var sessions: EncryptedSessionStore

    @Before
    fun setUp() {
        store = TestSessionStore()
        sessions = store.store()
    }

    @Test
    fun `a store that was never written reads as signed out`() = runTest {
        assertFalse(store.file.exists())

        assertNull(sessions.state.first())
    }

    @Test
    fun `a session survives being written and read back`() = runTest {
        sessions.save(ADA)

        // A second store over the same file: the first one holding it in memory would make
        // this pass without anything having been persisted.
        assertEquals(ADA, store.store().state.first())
    }

    @Test
    fun `signing out clears the stored session`() = runTest {
        sessions.save(ADA)

        sessions.clear()

        assertNull(sessions.state.first())
    }

    @Test
    fun `the address and the tokens are not readable in the file`() = runTest {
        sessions.save(ADA.copy(tokens = TOKENS))

        // The whole reason the session is in an encrypted store: a copy of the data directory
        // must not say who was signed in.
        val bytes = store.file.readBytes()
        assertTrue(bytes.isNotEmpty())
        assertFalse(bytes.decodeToString().contains("ada@example.com"))
        assertFalse(bytes.decodeToString().contains("1c2f"))
        assertFalse(bytes.decodeToString().contains("access-1"))
    }

    @Test
    fun `a session another installation's key wrote reads as signed out`() = runTest {
        sessions.save(ADA)
        // What restoring a backup to a new device does: the file arrives, the Keystore key does
        // not. Nothing here may throw — the honest answer is that there is no session any more.
        val restored = TestSessionStore(key = newAesKey())
        restored.file.parentFile?.mkdirs()
        store.file.copyTo(restored.file, overwrite = true)

        assertNull(restored.store().state.first())
    }

    @Test
    fun `a session in a format this build does not know reads as signed out`() = runTest {
        // Written through the store, so it is a properly encrypted value of an older shape — the
        // upgrade case, not the corruption one. Version 0, and one field too few.
        store.provider.dataStore.updateData { listOf("0", ADA.email).joinToString(SEPARATOR) }

        assertNull(sessions.state.first())
        assertTrue(store.logger.warnings.isNotEmpty())
    }

    @Test
    fun `a session with a separator in the address reads as signed out rather than truncated`() =
        runTest {
            // Not reachable through setSession today, but the decoder is what stands between a
            // malformed file and a Session carrying half an address.
            store.provider.dataStore.updateData {
                listOf("1", ADA.id, "ada", "example.com").joinToString(SEPARATOR)
            }

            assertNull(sessions.state.first())
        }

    @Test
    fun `a session stored before terms were versioned reads as having accepted the first`() = runTest {
        store.provider.dataStore.updateData { listOf("1", ADA.id, ADA.email).joinToString(SEPARATOR) }

        assertEquals(ADA.copy(termsVersion = 1), sessions.state.first())
    }

    @Test
    fun `the accepted terms version survives being written and read back`() = runTest {
        sessions.save(ADA.copy(termsVersion = 3))

        assertEquals(3, sessions.state.first()?.termsVersion)
    }

    @Test
    fun `the tokens survive being written and read back, through the token store too`() = runTest {
        sessions.save(ADA.copy(tokens = TOKENS))

        val reread = store.store()
        assertEquals(TOKENS, reread.state.first()?.tokens)
        assertEquals("access-1", reread.accessToken())
        assertEquals("refresh-1", reread.refreshToken())
    }

    @Test
    fun `a session stored before tokens were kept reads as signed in with none`() = runTest {
        store.provider.dataStore.updateData { listOf("2", ADA.id, ADA.email, "3").joinToString(SEPARATOR) }

        assertEquals(ADA.copy(termsVersion = 3), sessions.state.first())
        assertNull(sessions.accessToken())
    }

    @Test
    fun `a refreshed pair goes into the session it belongs to`() = runTest {
        sessions.save(ADA.copy(tokens = TOKENS))

        sessions.save(accessToken = "access-2", refreshToken = "refresh-2")

        assertEquals(Tokens("access-2", "refresh-2"), sessions.state.first()?.tokens)
        assertEquals(ADA.email, sessions.state.first()?.email)
    }

    @Test
    fun `a refresh that finishes after a sign-out does not sign anyone back in`() = runTest {
        sessions.save(ADA.copy(tokens = TOKENS))
        sessions.clear()

        sessions.save(accessToken = "access-2", refreshToken = "refresh-2")

        assertNull(sessions.state.first())
    }

    @Test
    fun `a refresh that returns no new refresh token keeps none`() = runTest {
        sessions.save(ADA.copy(tokens = TOKENS))

        sessions.save(accessToken = "access-2", refreshToken = null)

        assertNull(sessions.refreshToken())
        assertEquals("access-2", sessions.accessToken())
    }
}
