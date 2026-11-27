package com.example.androidproject1.feature.auth.data.source

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:auth:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 *
 * The auth feature's first request. Sign-in is still the mock the template ships with —
 * a session recorded locally, verifying nothing — and deletion is the one call that has to reach
 * a server, because a server that never heard the account was gone would keep it.
 */
interface RemoteAuthDataSource {

    /** `DELETE account` on the base URL. Returns when the server has said 2xx; throws a `DomainError` otherwise. */
    suspend fun deleteAccount()

    /** `POST auth/password/reset` with `{"email": …}`. Returns on 2xx; throws a `DomainError` otherwise. */
    suspend fun requestPasswordReset(email: String)

    /** `POST auth/password` with `{"token": …, "password": …}`. Returns on 2xx; throws a `DomainError` otherwise. */
    suspend fun resetPassword(token: String, password: String)

    /** `POST auth/signup` with `{"email": …}`: the account, and a code mailed. */
    suspend fun signUp(email: String)

    /** `POST auth/verify` with `{"email": …, "code": …}`. */
    suspend fun verifyEmail(email: String, code: String)

    /** `POST auth/verify/resend` with `{"email": …}`. */
    suspend fun resendVerification(email: String)
}
