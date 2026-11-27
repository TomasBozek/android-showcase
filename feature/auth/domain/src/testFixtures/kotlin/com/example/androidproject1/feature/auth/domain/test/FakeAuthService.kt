package com.example.androidproject1.feature.auth.domain.test

import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.domain.Session
import com.example.androidproject1.service.core.domain.error.BadRequestError
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [AuthService] for tests. Set [failWith] to make the next call fail.
 *
 * A fixture of `:feature:auth:domain` rather than of one test source set, because the session is
 * cross-cutting: `:feature:settings` reads it too, and had grown its own near-copy.
 *
 * @property session settable, so a test can start from a signed-in state without calling [login].
 * @property sessionError set to make the session flows emit a failure instead of the session. Kept
 * apart from [failWith] so a test of a failing sign-in still gets a readable session.
 */
class FakeAuthService(var failWith: DomainError? = null) : AuthService {

    val session = MutableStateFlow<Session?>(null)

    var sessionError: DomainError? = null

    val loggedInEmails = mutableListOf<String>()

    var logoutCount = 0
        private set

    override fun observeSession(): Flow<Outcome<Session?>> =
        session.map { sessionOutcome(it) }

    override fun isLoggedIn(): Flow<Outcome<Boolean>> =
        session.map { sessionOutcome(it != null) }

    override suspend fun login(email: String): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        loggedInEmails += email
        // A fixed id rather than a random one: a test that asserts what reached the crash
        // reporter needs a value it can name.
        session.value = Session(id = SESSION_ID, email = email.ifBlank { "guest@example.com" })
        return Outcome.Success(Unit)
    }

    override suspend fun logout(): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        logoutCount++
        session.value = null
        return Outcome.Success(Unit)
    }

    var deleteAccountCount = 0
        private set

    /** Every address a reset was asked for, in order. */
    val passwordResets = mutableListOf<String>()

    override suspend fun acceptTerms(version: Int): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        session.value = session.value?.copy(termsVersion = version)
        return Outcome.Success(Unit)
    }

    /** Every address an account was created for; none of them has a session until verified. */
    val signUps = mutableListOf<String>()
    val verificationResends = mutableListOf<String>()

    /** The one code [verifyEmail] accepts, as the `dev` fixture does. */
    var acceptedCode = "000000"

    override suspend fun signUp(email: String): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        signUps += email
        return Outcome.Success(Unit)
    }

    override suspend fun verifyEmail(email: String, code: String): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        if (code != acceptedCode) return Outcome.Failure(BadRequestError(message = "Bad Request"))
        return login(email)
    }

    override suspend fun resendVerification(email: String): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        verificationResends += email
        return Outcome.Success(Unit)
    }

    /** Every token a new password was set with, in order. */
    val passwordsSet = mutableListOf<String>()

    override suspend fun resetPassword(token: String, password: String): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        passwordsSet += token
        return Outcome.Success(Unit)
    }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        passwordResets += email
        return Outcome.Success(Unit)
    }

    override suspend fun deleteAccount(): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        deleteAccountCount++
        session.value = null
        return Outcome.Success(Unit)
    }

    private fun <T> sessionOutcome(value: T): Outcome<T> =
        sessionError?.let { Outcome.Failure(it) } ?: Outcome.Success(value)

    companion object {

        const val SESSION_ID = "session-1"
    }
}
