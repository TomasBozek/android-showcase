package com.example.androidproject1.feature.auth.data.source

import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.network.HttpErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Switching to IO is this class's job, not the repository's: `BaseRepository` runs on the caller's
 * context and the caller is `viewModelScope`, which is `Dispatchers.Main`.
 *
 * Failures are mapped here so the repository above sees a `DomainError` and never an HTTP status.
 * The client already throws on a non-2xx status, so a 204 is the whole of a good answer and
 * nothing is read from it.
 */
class DefaultRemoteAuthDataSource(
    private val client: HttpClient,
    private val dispatcherProvider: DispatcherProvider,
) : RemoteAuthDataSource {

    override suspend fun deleteAccount() = withContext(dispatcherProvider.io) {
        try {
            client.delete("account")
            Unit
        } catch (throwable: Throwable) {
            throw HttpErrorMapper.map(throwable)
        }
    }

    override suspend fun requestPasswordReset(email: String) = withContext(dispatcherProvider.io) {
        try {
            client.post("auth/password/reset") {
                contentType(ContentType.Application.Json)
                setBody(EmailRequest(email = email))
            }
            Unit
        } catch (throwable: Throwable) {
            throw HttpErrorMapper.map(throwable)
        }
    }

    override suspend fun resetPassword(token: String, password: String) =
        post("auth/password", NewPasswordRequest(token = token, password = password))

    override suspend fun signUp(email: String) = post("auth/signup", EmailRequest(email = email))

    override suspend fun verifyEmail(email: String, code: String) = post(
        "auth/verify",
        VerifyRequest(email = email, code = code),
    )

    override suspend fun resendVerification(email: String) = post("auth/verify/resend", EmailRequest(email = email))

    /** A JSON body to [path], on IO; any refusal comes out a `DomainError`. Nothing is read from a 2xx. */
    private suspend inline fun <reified T : Any> post(path: String, body: T) = withContext(dispatcherProvider.io) {
        try {
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            Unit
        } catch (throwable: Throwable) {
            throw HttpErrorMapper.map(throwable)
        }
    }
}

/** The code's body. */
@Serializable
private data class VerifyRequest(val email: String, val code: String)

/** The new password's body. */
@Serializable
private data class NewPasswordRequest(val token: String, val password: String)

/** A body that is just an address — the reset request, sign-up, a resend. The wire's shape, so it stays in the data layer. */
@Serializable
private data class EmailRequest(val email: String)
