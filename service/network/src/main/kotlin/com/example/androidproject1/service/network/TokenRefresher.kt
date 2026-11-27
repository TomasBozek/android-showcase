package com.example.androidproject1.service.network

/**
 * Exchanges a refresh token for a new pair.
 *
 * Separate from [TokenStore] because storing and refreshing are different concerns with different
 * owners: the store is this app's persistence, the refresher is the API's contract. A project with
 * no refresh endpoint binds one that always returns null and keeps the store.
 */
public interface TokenRefresher {

    /** Returns the new tokens, or `null` when the refresh token is no longer good. */
    public suspend fun refresh(refreshToken: String): Tokens?
}

public data class Tokens(
    val accessToken: String,
    val refreshToken: String?,
)
