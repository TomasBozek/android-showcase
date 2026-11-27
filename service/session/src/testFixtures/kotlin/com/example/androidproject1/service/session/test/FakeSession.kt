package com.example.androidproject1.service.session.test

import com.example.androidproject1.service.network.TokenStore
import com.example.androidproject1.service.network.Tokens
import com.example.androidproject1.service.session.Session
import com.example.androidproject1.service.session.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** An in-memory [SessionStore] and [TokenStore] a test scripts, beside the ports it fakes. */
class FakeSession(initial: Session? = null) : SessionStore, TokenStore {

    private val current = MutableStateFlow(initial)

    /** What is stored now, for a test to assert on. */
    val session: Session? get() = current.value

    override val state: Flow<Session?> = current

    override suspend fun save(session: Session) {
        current.value = session
    }

    override suspend fun clear() {
        current.value = null
    }

    override suspend fun accessToken(): String? = current.value?.tokens?.accessToken

    override suspend fun refreshToken(): String? = current.value?.tokens?.refreshToken

    override suspend fun save(accessToken: String, refreshToken: String?) {
        current.value = current.value?.copy(tokens = Tokens(accessToken, refreshToken))
    }
}
