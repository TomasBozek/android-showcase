package com.example.androidproject1.feature.auth.domain

import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.error.NetworkError
import com.example.androidproject1.service.core.domain.error.UnexpectedError
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The order deletion runs in, which is the whole of what the service adds over the
 * repository: the request first, every eraser next, the session last — and a failure anywhere
 * before the last step leaves the session where it was.
 */
class DefaultAuthServiceTest {

    /** Records what was called, in order, so the sequence itself is what is asserted. */
    private val calls = mutableListOf<String>()

    private inner class RecordingRepository(
        var deleteFailsWith: NetworkError? = null,
    ) : AuthRepository {

        val session = MutableStateFlow<Session?>(Session(id = "s", email = "ada@example.com"))

        override fun observeSession(): Flow<Outcome<Session?>> = session.map { Outcome.Success(it) }

        override suspend fun login(email: String): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun logout(): Outcome<Unit> {
            calls += "logout"
            session.value = null
            return Outcome.Success(Unit)
        }

        override suspend fun deleteAccount(): Outcome<Unit> {
            calls += "delete"
            return deleteFailsWith?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)
        }

        override suspend fun signUp(email: String): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun acceptTerms(version: Int): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun verifyEmail(email: String, code: String): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun resendVerification(email: String): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun resetPassword(token: String, password: String): Outcome<Unit> {
            calls += "set $token"
            return Outcome.Success(Unit)
        }

        override suspend fun requestPasswordReset(email: String): Outcome<Unit> {
            calls += "reset $email"
            return Outcome.Success(Unit)
        }
    }

    private fun eraser(name: String, failing: Boolean = false, onSignOut: Boolean = true) = object : LocalDataEraser {
        override val erasesOnSignOut = onSignOut

        override suspend fun erase() {
            calls += "erase:$name"
            if (failing) error("$name could not be wiped")
        }
    }

    @Test
    fun `the remote delete runs first, the erasers next and the session is cleared last`() = runTest {
        val repository = RecordingRepository()
        val service = DefaultAuthService(repository, listOf(eraser("cart"), eraser("profile")))

        val outcome = service.deleteAccount()

        assertTrue(outcome is Outcome.Success)
        assertEquals(listOf("delete", "erase:cart", "erase:profile", "logout"), calls)
        assertNull(repository.session.value)
    }

    @Test
    fun `a failing eraser keeps the session`() = runTest {
        val repository = RecordingRepository()
        val service = DefaultAuthService(repository, listOf(eraser("cart", failing = true), eraser("profile")))

        val outcome = service.deleteAccount()

        // The failure is the reply, the erasers after it never run, and nobody is signed out
        // over a half-wiped device.
        assertTrue(outcome is Outcome.Failure)
        assertTrue((outcome as Outcome.Failure).error is UnexpectedError)
        assertEquals(listOf("delete", "erase:cart"), calls)
        assertNotNull(repository.session.value)
    }

    @Test
    fun `a refused request erases nothing`() = runTest {
        val repository = RecordingRepository(deleteFailsWith = NetworkError())
        val service = DefaultAuthService(repository, listOf(eraser("cart")))

        val outcome = service.deleteAccount()

        assertTrue(outcome is Outcome.Failure)
        assertEquals(listOf("delete"), calls)
        assertNotNull(repository.session.value)
    }

    @Test
    fun `a sign-out erases every feature's data before the session is cleared`() = runTest {
        val repository = RecordingRepository()
        val service = DefaultAuthService(repository, listOf(eraser("cart"), eraser("profile")))

        val outcome = service.logout()

        assertTrue(outcome is Outcome.Success)
        assertEquals(listOf("erase:cart", "erase:profile", "logout"), calls)
        assertNull(repository.session.value)
    }

    @Test
    fun `a sign-out leaves what is the install's rather than the account's, and a deletion takes it too`() = runTest {
        val erasers = listOf(eraser("cart"), eraser("installId", onSignOut = false))

        DefaultAuthService(RecordingRepository(), erasers).logout()
        assertEquals(listOf("erase:cart", "logout"), calls)

        calls.clear()
        DefaultAuthService(RecordingRepository(), erasers).deleteAccount()
        assertEquals(listOf("delete", "erase:cart", "erase:installId", "logout"), calls)
    }

    @Test
    fun `a sign-out still ends the session when an eraser fails, and says so`() = runTest {
        val repository = RecordingRepository()
        val service = DefaultAuthService(repository, listOf(eraser("cart", failing = true), eraser("profile")))

        val outcome = service.logout()

        // Unlike a deletion, a sign-out cannot be refused: the rest are still wiped, the session
        // still ends, and the failure is the reply.
        assertTrue(outcome is Outcome.Failure)
        assertTrue((outcome as Outcome.Failure).error is UnexpectedError)
        assertEquals(listOf("erase:cart", "erase:profile", "logout"), calls)
        assertNull(repository.session.value)
    }

    @Test
    fun `a reset request goes to the repository and touches nothing else`() = runTest {
        DefaultAuthService(RecordingRepository(), listOf(eraser("cart"))).requestPasswordReset("ada@example.com")

        assertEquals(listOf("reset ada@example.com"), calls)
    }
}
