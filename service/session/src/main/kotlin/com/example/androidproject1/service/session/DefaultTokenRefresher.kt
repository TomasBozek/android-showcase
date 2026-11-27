package com.example.androidproject1.service.session

import com.example.androidproject1.service.network.TokenRefresher
import com.example.androidproject1.service.network.Tokens
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * `POST auth/refresh` with the refresh token; the new pair, or `null` when the server refuses it.
 *
 * [client] must be one **without** the bearer plugin: a refresh that itself answers `401` would
 * otherwise ask to be refreshed. Only a refusal — a 4xx — is `null`. A transport failure is
 * thrown, so a phone in a tunnel fails the request that needed the token instead of ending a
 * session the server never rejected.
 */
public class DefaultTokenRefresher(
    private val client: HttpClient,
    private val path: String = REFRESH_PATH,
) : TokenRefresher {

    override suspend fun refresh(refreshToken: String): Tokens? = try {
        val pair = client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken = refreshToken))
        }.body<RefreshResponse>()
        Tokens(accessToken = pair.accessToken, refreshToken = pair.refreshToken)
    } catch (refused: ResponseException) {
        if (refused.response.status.value in CLIENT_ERRORS) null else throw refused
    }

    @Serializable
    private data class RefreshRequest(val refreshToken: String)

    @Serializable
    private data class RefreshResponse(val accessToken: String, val refreshToken: String? = null)

    public companion object {

        public const val REFRESH_PATH: String = "auth/refresh"
        private val CLIENT_ERRORS = 400..499
    }
}
