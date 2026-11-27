package com.example.androidproject1.service.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** In-memory [TokenStore]. The mutex is so a parallel-refresh test is not racing on the fake. */
class FakeTokenStore(
    private var access: String? = "stale-access",
    private var refresh: String? = "refresh-1",
) : TokenStore {

    private val lock = Mutex()

    override suspend fun accessToken(): String? = lock.withLock { access }

    override suspend fun refreshToken(): String? = lock.withLock { refresh }

    override suspend fun save(accessToken: String, refreshToken: String?) = lock.withLock {
        access = accessToken
        refresh = refreshToken
    }

    /** What a sign-out (both `null`) or the next sign-in does to the store under the client. */
    suspend fun replace(accessToken: String?, refreshToken: String?) = lock.withLock {
        access = accessToken
        refresh = refreshToken
    }
}
