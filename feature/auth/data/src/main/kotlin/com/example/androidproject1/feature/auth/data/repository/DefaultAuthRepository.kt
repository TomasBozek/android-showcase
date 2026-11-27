package com.example.androidproject1.feature.auth.data.repository

import com.example.androidproject1.feature.auth.data.source.RemoteAuthDataSource
import com.example.androidproject1.feature.auth.domain.AuthRepository
import com.example.androidproject1.feature.auth.domain.Session
import com.example.androidproject1.feature.auth.domain.TermsPolicy
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.network.Tokens
import com.example.androidproject1.service.session.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import com.example.androidproject1.service.session.Session as SessionRecord

class DefaultAuthRepository(
    logger: Logger,
    private val sessionStore: SessionStore,
    private val remoteAuthDataSource: RemoteAuthDataSource,
    /** Overridable so a test does not have to assert against a value it cannot predict. */
    private val newSessionId: () -> String = { UUID.randomUUID().toString() },
    /** What a sign-in or a verified sign-up has accepted: the terms this build shows. */
    private val currentTermsVersion: Int = TermsPolicy.FIRST_VERSION,
    /**
     * What a sign-in is given to prove it. This app has no sign-in endpoint, so it makes an
     * opaque pair the way it makes the session id; a real one is the server's answer.
     */
    private val newTokens: () -> Tokens = { Tokens(accessToken = randomToken(), refreshToken = randomToken()) },
) : AuthRepository, BaseRepository(logger = logger.withTag("DefaultAuthRepository")) {

    // MainViewModel collects this for the whole process lifetime, so one transient read failure
    // would otherwise end the flow for good.
    override fun observeSession(): Flow<Outcome<Session?>> =
        observe(
            source = sessionStore.state.map { stored ->
                stored?.let { Session(id = it.id, email = it.email, termsVersion = it.termsVersion) }
            },
            retries = SESSION_RETRIES,
        )

    /**
     * Mints an opaque id at sign-in.
     *
     * A random id rather than a hash of the address: a hash is still the address to anyone holding
     * a list of addresses, and this one is going to a crash reporter.
     */
    override suspend fun login(email: String): Outcome<Unit> = execute {
        sessionStore.save(
            // A sign-in agrees to the terms the server holds for the account; this mock has no
            // server to ask, so it takes the current ones — or every sign-in would be re-asked.
            SessionRecord(
                id = newSessionId(),
                email = email.ifBlank {
                    ANONYMOUS_EMAIL
                },
                termsVersion = currentTermsVersion,
                tokens = newTokens(),
            ),
        )
    }

    override suspend fun logout(): Outcome<Unit> = execute {
        sessionStore.clear()
    }

    // The request and nothing else: the session is `logout()`'s, called last by the service.
    override suspend fun deleteAccount(): Outcome<Unit> = execute {
        remoteAuthDataSource.deleteAccount()
    }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit> = execute {
        remoteAuthDataSource.requestPasswordReset(email.trim())
    }

    override suspend fun signUp(email: String): Outcome<Unit> = execute {
        remoteAuthDataSource.signUp(email.trim())
    }

    // The session starts only here, after the server has seen the code — the same local
    // record `login` writes.
    override suspend fun verifyEmail(email: String, code: String): Outcome<Unit> = execute {
        remoteAuthDataSource.verifyEmail(email.trim(), code)
        // Sign-up ticked the box for the current terms; the session records it.
        sessionStore.save(
            SessionRecord(
                id = newSessionId(),
                email = email.trim(),
                termsVersion = currentTermsVersion,
                tokens = newTokens(),
            ),
        )
    }

    override suspend fun acceptTerms(version: Int): Outcome<Unit> = execute {
        val stored = sessionStore.state.first() ?: return@execute
        sessionStore.save(stored.copy(termsVersion = version))
    }

    override suspend fun resendVerification(email: String): Outcome<Unit> = execute {
        remoteAuthDataSource.resendVerification(email.trim())
    }

    // The password is sent as typed: a trailing space may be part of it.
    override suspend fun resetPassword(token: String, password: String): Outcome<Unit> = execute {
        remoteAuthDataSource.resetPassword(token, password)
    }

    private companion object {

        /** Used when the login form is skipped. */
        const val ANONYMOUS_EMAIL = "guest@example.com"

        const val SESSION_RETRIES = 3L

        fun randomToken() = UUID.randomUUID().toString()
    }
}
