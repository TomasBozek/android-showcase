package com.example.androidproject1.feature.auth.domain

import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

/** Implemented in the data layer; declared here so domain depends on nothing. */
interface AuthRepository {

    fun observeSession(): Flow<Outcome<Session?>>

    suspend fun login(email: String): Outcome<Unit>

    suspend fun logout(): Outcome<Unit>

    /**
     * Asks the server to delete the account — and only that. The session is cleared by
     * [logout], which `AuthService` calls last, after every feature has erased its share.
     */
    suspend fun deleteAccount(): Outcome<Unit>

    /**
     * Asks the server to mail a reset link to [email]. Succeeds whether or not an account has that
     * address — a server that said otherwise would tell anyone which addresses are registered.
     */
    suspend fun requestPasswordReset(email: String): Outcome<Unit>

    /**
     * Sets a new password with the [token] a reset link carried. A token the server does not
     * recognise — expired, used, or never issued — is a `BadRequestError`.
     */
    suspend fun resetPassword(token: String, password: String): Outcome<Unit>

    /**
     * Creates the account and has a code mailed to [email]. Returns **without a session**: the
     * session exists only once [verifyEmail] has seen the code.
     */
    suspend fun signUp(email: String): Outcome<Unit>

    /** Checks the mailed [code] and, when it is right, starts the session. A wrong one is a `BadRequestError`. */
    suspend fun verifyEmail(email: String, code: String): Outcome<Unit>

    /** Another code to [email], for one that did not arrive. */
    suspend fun resendVerification(email: String): Outcome<Unit>

    /** Records that the signed-in account accepted terms [version]. Nothing when signed out. */
    suspend fun acceptTerms(version: Int): Outcome<Unit>
}
