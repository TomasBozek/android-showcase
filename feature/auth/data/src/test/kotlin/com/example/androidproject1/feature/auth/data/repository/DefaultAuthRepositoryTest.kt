package com.example.androidproject1.feature.auth.data.repository

import com.example.androidproject1.feature.auth.data.source.RemoteAuthDataSource
import com.example.androidproject1.service.core.domain.error.BadRequestError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.session.Session
import com.example.androidproject1.service.session.SessionStore
import com.example.androidproject1.service.session.test.FakeSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private const val ADA = "ada@example.com"

/**
 * Sign-in against `FakeSession`: what is worth an assertion is that signing in leaves something
 * the next reader — `MainViewModel`, on the next cold start — actually finds. That a session
 * survives a disk is `EncryptedSessionStoreTest`'s.
 */
class DefaultAuthRepositoryTest {

    private val store = FakeSession()

    /** Counts the requests; nothing here is about the wire, which `DefaultRemoteAuthDataSource` and the engine cover. */
    private val remote = object : RemoteAuthDataSource {
        var deletes = 0

        override suspend fun deleteAccount() {
            deletes++
        }

        val resets = mutableListOf<String>()

        override suspend fun requestPasswordReset(email: String) {
            resets += email
        }

        override suspend fun resetPassword(token: String, password: String) = Unit

        var refuseCode = false

        override suspend fun signUp(email: String) = Unit

        override suspend fun verifyEmail(email: String, code: String) {
            if (refuseCode) throw BadRequestError(message = "Bad Request")
        }

        override suspend fun resendVerification(email: String) = Unit
    }

    private fun repository(
        source: SessionStore = store,
        newSessionId: (() -> String)? = null,
    ) = if (newSessionId == null) {
        // The real generator, so the id test below is about what ships rather than about a
        // lambda this test just handed it.
        DefaultAuthRepository(logger = FakeLogger(), sessionStore = source, remoteAuthDataSource = remote)
    } else {
        DefaultAuthRepository(FakeLogger(), source, remote, newSessionId)
    }

    /** The request and nothing else — the session is the service's to end, last. */
    @Test
    fun `deleting the account asks the server and keeps the session`() = runTest {
        repository().login("ada@example.com")

        val outcome = repository().deleteAccount()

        assertTrue(outcome is Outcome.Success)
        assertEquals(1, remote.deletes)
        assertEquals("ada@example.com", session()?.email)
    }

    private suspend fun session() =
        (repository().observeSession().first() as Outcome.Success).data

    @Test
    fun `signing in leaves a session the next reader finds`() = runTest {
        repository(newSessionId = { "session-1" }).login(ADA)

        assertEquals("session-1", session()?.id)
        assertEquals(ADA, session()?.email)
    }

    @Test
    fun `signing in stores the tokens the client will send, with the session`() = runTest {
        repository(newSessionId = { "session-1" }).login(ADA)

        val tokens = store.session?.tokens
        assertTrue(tokens != null && tokens.accessToken.isNotBlank() && !tokens.refreshToken.isNullOrBlank())
        assertEquals(tokens?.accessToken, store.accessToken())
    }

    @Test
    fun `signing out takes the tokens with it`() = runTest {
        val repository = repository()
        repository.login(ADA)

        repository.logout()

        assertNull(store.accessToken())
        assertNull(store.refreshToken())
    }

    @Test
    fun `accepting terms keeps the tokens`() = runTest {
        val repository = repository()
        repository.login(ADA)
        val before = store.session?.tokens

        repository.acceptTerms(2)

        assertEquals(before, store.session?.tokens)
    }

    @Test
    fun `nobody is signed in until somebody signs in`() = runTest {
        assertNull(session())
    }

    @Test
    fun `signing out clears the session`() = runTest {
        val repository = repository()
        repository.login(ADA)

        repository.logout()

        assertNull(session())
    }

    @Test
    fun `skipping the form signs in as a guest rather than as nobody`() = runTest {
        // A blank address must not read as signed out, or the app bounces straight back to login.
        repository().login("   ")

        assertEquals("guest@example.com", session()?.email)
    }

    @Test
    fun `the session id says nothing about the address`() = runTest {
        // The id is what reaches the crash reporter, so this is the privacy property rather than
        // a detail: a value derived from the address is the address to anyone holding a list.
        repository().login(ADA)
        val first = session()?.id.orEmpty()

        repository().login(ADA)
        val second = session()?.id.orEmpty()

        // Not a hash of the address either: the same address twice gives two ids, which no
        // function of the address alone can do.
        assertNotEquals(first, second)
        assertFalse(first.contains(ADA))
    }

    @Test
    fun `a read failure is reported rather than thrown`() = runTest {
        val outcome = repository(source = FlakySessionStore(readFailures = Int.MAX_VALUE))
            .observeSession()
            .first()

        assertTrue(outcome is Outcome.Failure)
    }

    @Test
    fun `a transient read failure does not end the session flow`() = runTest {
        // MainViewModel collects this for the whole process lifetime. Without the retries the
        // repository asks for, one unlucky read would sign the user out until they restarted.
        val source = FlakySessionStore(readFailures = 2)
        val repository = repository(source = source, newSessionId = { "session-1" })
        repository.login(ADA)

        val outcome = repository.observeSession().first()

        assertEquals("session-1", (outcome as Outcome.Success).data?.id)
    }

    @Test
    fun `a write failure is reported rather than thrown`() = runTest {
        val repository = repository(source = FlakySessionStore(failWrites = true))

        assertTrue(repository.login(ADA) is Outcome.Failure)
        assertTrue(repository.logout() is Outcome.Failure)
    }

    @Test
    fun `sign-up starts no session, and a right code does`() = runTest {
        val repository = repository()

        repository.signUp("ada@example.com")
        assertNull(store.state.first())

        repository.verifyEmail("ada@example.com", "000000")
        assertEquals("ada@example.com", store.state.first()?.email)
    }

    @Test
    fun `accepting new terms is stored with the session`() = runTest {
        val repository = repository()
        repository.login("ada@example.com")

        repository.acceptTerms(2)

        assertEquals(2, repository.observeSession().first().let { (it as Outcome.Success).data?.termsVersion })
    }

    @Test
    fun `a wrong code starts no session`() = runTest {
        remote.refuseCode = true
        val repository = repository()

        val outcome = repository.verifyEmail("ada@example.com", "999999")

        assertTrue(outcome is Outcome.Failure)
        assertNull(store.state.first())
    }
}

/**
 * A store that fails a fixed number of times before working.
 *
 * A store that always fails cannot tell a retried flow from an abandoned one, which is the whole
 * question `retries` exists to answer.
 */
private class FlakySessionStore(
    private val readFailures: Int = 0,
    private val failWrites: Boolean = false,
) : SessionStore {

    private val stored = MutableStateFlow<Session?>(null)

    private var reads = 0

    override val state: Flow<Session?> = flow {
        if (reads++ < readFailures) throw IOException("the disk is busy")
        emitAll(stored)
    }

    override suspend fun save(session: Session) {
        if (failWrites) throw IOException("the disk is full")
        stored.value = session
    }

    override suspend fun clear() {
        if (failWrites) throw IOException("the disk is full")
        stored.value = null
    }
}
