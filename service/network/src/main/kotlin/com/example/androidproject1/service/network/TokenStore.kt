package com.example.androidproject1.service.network

/**
 * Where the client gets its tokens and where a refreshed pair goes back to.
 *
 * An interface, and in this module rather than in a feature, because `:service:network` must not
 * know how this app stores a session — `feat.6`'s encrypted DataStore, someone else's Keychain.
 * What it knows is that a token can be read and that a refreshed pair goes back.
 *
 * There is no `clear`: what ends a session is [SessionEnder]'s, said once, and clearing the store
 * is the owner's answer to it. A store that cleared itself on a failed refresh would sign the user
 * out before the owner had been told why.
 */
public interface TokenStore {

    public suspend fun accessToken(): String?

    public suspend fun refreshToken(): String?

    public suspend fun save(accessToken: String, refreshToken: String?)
}
