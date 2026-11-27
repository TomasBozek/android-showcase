package com.example.androidproject1.feature.auth.domain

import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.error.UnexpectedError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.domain.result.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The auth feature's public API — the only part other features may depend on. */
interface AuthService {

    fun observeSession(): Flow<Outcome<Session?>>

    fun isLoggedIn(): Flow<Outcome<Boolean>>

    /** Mock sign-in: records the session locally, verifying nothing. */
    suspend fun login(email: String): Outcome<Unit>

    suspend fun logout(): Outcome<Unit>

    /**
     * Deletes the account: the request first, then every feature's local data, then the session.
     * A failure anywhere before the last step leaves the session in place, so the device is
     * never signed out over a half-wiped store — the alert says so and the user can try again.
     */
    suspend fun deleteAccount(): Outcome<Unit>

    /** A reset link to [email]; see [AuthRepository.requestPasswordReset]. Signs nobody in or out. */
    suspend fun requestPasswordReset(email: String): Outcome<Unit>

    /** The new password, with the link's token; see [AuthRepository.resetPassword]. Signs nobody in. */
    suspend fun resetPassword(token: String, password: String): Outcome<Unit>

    /** Creates the account; no session until [verifyEmail]. See [AuthRepository.signUp]. */
    suspend fun signUp(email: String): Outcome<Unit>

    /** The mailed code; the session starts here. See [AuthRepository.verifyEmail]. */
    suspend fun verifyEmail(email: String, code: String): Outcome<Unit>

    suspend fun resendVerification(email: String): Outcome<Unit>

    /** The updated terms were accepted; see [AuthRepository.acceptTerms]. */
    suspend fun acceptTerms(version: Int): Outcome<Unit>
}

class DefaultAuthService(
    private val authRepository: AuthRepository,
    /** Every feature's share of the wipe, collected by the DI module with `getAll()`. */
    private val erasers: List<LocalDataEraser>,
) : AuthService {

    override fun observeSession(): Flow<Outcome<Session?>> = authRepository.observeSession()

    override fun isLoggedIn(): Flow<Outcome<Boolean>> =
        authRepository.observeSession().map { result -> result.map { it != null } }

    override suspend fun login(email: String): Outcome<Unit> = authRepository.login(email)

    /**
     * Every feature's data goes with the session, so the next account on this phone starts from
     * nothing — the same erasers as [deleteAccount] bar the install's own, before the session clears. A sign-out cannot
     * be refused, so an eraser that fails does not stop the rest or keep the session: the session
     * still ends, and the first failure is the reply.
     */
    override suspend fun logout(): Outcome<Unit> {
        var failure: Outcome.Failure? = null
        erasers.filter { it.erasesOnSignOut }.forEach { eraser ->
            try {
                eraser.erase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e as? DomainError ?: UnexpectedError(message = e.message, cause = e)
                failure = failure ?: Outcome.Failure(error)
            }
        }
        val signedOut = authRepository.logout()
        return failure ?: signedOut
    }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit> =
        authRepository.requestPasswordReset(email)

    override suspend fun resetPassword(token: String, password: String): Outcome<Unit> =
        authRepository.resetPassword(token, password)

    override suspend fun signUp(email: String): Outcome<Unit> = authRepository.signUp(email)

    override suspend fun verifyEmail(email: String, code: String): Outcome<Unit> = authRepository.verifyEmail(
        email,
        code,
    )

    override suspend fun resendVerification(email: String): Outcome<Unit> = authRepository.resendVerification(email)

    override suspend fun acceptTerms(version: Int): Outcome<Unit> = authRepository.acceptTerms(version)

    override suspend fun deleteAccount(): Outcome<Unit> {
        val deleted = authRepository.deleteAccount()
        if (deleted is Outcome.Failure) return deleted
        // Sequential, not in parallel: the order is not the point, but a failure stopping the
        // rest is — and an eraser's error, unlike a repository's, is a thrown exception, which
        // is what the domain has for it since the erasers hand back nothing.
        try {
            erasers.forEach { it.erase() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Outcome.Failure(e as? DomainError ?: UnexpectedError(message = e.message, cause = e))
        }
        return authRepository.logout()
    }
}
