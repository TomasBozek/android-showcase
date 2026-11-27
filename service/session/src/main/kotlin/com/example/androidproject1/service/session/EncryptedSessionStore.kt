package com.example.androidproject1.service.session

import com.example.androidproject1.service.core.data.EncryptedDataStoreProvider
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.network.TokenStore
import com.example.androidproject1.service.network.Tokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The session, encrypted at rest — the [SessionStore] and the [TokenStore] the HTTP client reads,
 * one object over one file.
 *
 * It is in an [EncryptedDataStoreProvider] rather than the shared preferences store because it is
 * the one value here worth protecting: a copy of the app's data directory should not say who was
 * signed in or hand over a token.
 *
 * The store holds one string. An absent file, an undecryptable one and an empty one all read as
 * signed out, so there is no third case — and neither does a session written by a build with a
 * different format, which is what the version prefix is for: it is not parsed, it is not
 * recognised.
 */
public class EncryptedSessionStore(
    sessionStoreProvider: EncryptedDataStoreProvider,
    private val dispatcherProvider: DispatcherProvider,
    logger: Logger,
) : SessionStore, TokenStore {

    private val logger = logger.withTag("EncryptedSessionStore")
    private val dataStore = sessionStoreProvider.dataStore

    override val state: Flow<Session?> = dataStore.data
        .map { stored -> stored.takeIf { it.isNotEmpty() }?.let(::decode) }
        .flowOn(dispatcherProvider.io)

    override suspend fun save(session: Session): Unit = write(encode(session))

    override suspend fun clear(): Unit = write("")

    override suspend fun accessToken(): String? = current()?.tokens?.accessToken

    override suspend fun refreshToken(): String? = current()?.tokens?.refreshToken

    /**
     * A refreshed pair goes into the session it belongs to. With nobody signed in there is nothing
     * to attach it to — a refresh that finishes after a sign-out must not sign anyone back in.
     */
    override suspend fun save(accessToken: String, refreshToken: String?) {
        withContext(dispatcherProvider.io) {
            dataStore.updateData { stored ->
                val session = stored.takeIf { it.isNotEmpty() }?.let(::decode) ?: return@updateData stored
                encode(session.copy(tokens = Tokens(accessToken = accessToken, refreshToken = refreshToken)))
            }
        }
    }

    private suspend fun current(): Session? = state.first()

    private suspend fun write(value: String) {
        withContext(dispatcherProvider.io) { dataStore.updateData { value } }
    }

    private fun encode(session: Session): String {
        val fields = mutableListOf(VERSION.toString(), session.id, session.email, session.termsVersion.toString())
        session.tokens?.let {
            fields += it.accessToken
            fields += it.refreshToken.orEmpty()
        }
        return fields.joinToString(SEPARATOR)
    }

    /**
     * Returns null for anything this build does not recognise.
     *
     * The alternative — throwing — turns "you upgraded the app" into a crash loop on launch, and
     * the worst a null costs is one sign-in.
     */
    private fun decode(stored: String): Session? {
        val parts = stored.split(SEPARATOR)
        return when {
            // The shape before terms were versioned: read, not signed out, and taken to have
            // accepted the first terms. The next write stores the current shape.
            parts.size == V1_FIELD_COUNT && parts[0] == V1.toString() ->
                Session(id = parts[1], email = parts[2], termsVersion = FIRST_TERMS_VERSION, tokens = null)

            parts.size == V2_FIELD_COUNT && parts[0] == V2.toString() && parts[3].toIntOrNull() != null ->
                Session(id = parts[1], email = parts[2], termsVersion = parts[3].toInt(), tokens = null)

            parts.size in FIELD_COUNT_RANGE && parts[0] == VERSION.toString() && parts[3].toIntOrNull() != null ->
                Session(
                    id = parts[1],
                    email = parts[2],
                    termsVersion = parts[3].toInt(),
                    tokens = parts.getOrNull(ACCESS_INDEX)?.let {
                        Tokens(accessToken = it, refreshToken = parts.getOrNull(REFRESH_INDEX)?.ifEmpty { null })
                    },
                )

            else -> {
                logger.w { "Session in an unrecognised format; signing out" }
                null
            }
        }
    }

    private companion object {

        /**
         * Bump when the stored shape changes, and read the previous one in [decode] if signing
         * everybody out is not what the change is for.
         */
        const val VERSION = 3

        /** The shape before tokens were stored: id, address and terms version. */
        const val V2 = 2
        const val V2_FIELD_COUNT = 4

        /** The shape before the terms version was stored: id and address only. */
        const val V1 = 1
        const val V1_FIELD_COUNT = 3

        /** What an account with no recorded acceptance is taken to have accepted. */
        const val FIRST_TERMS_VERSION = 1

        /** With tokens: version, id, address, terms, access, refresh. Without: the first four. */
        val FIELD_COUNT_RANGE = 4..6
        const val ACCESS_INDEX = 4
        const val REFRESH_INDEX = 5

        /** ASCII unit separator: not in a UUID, not valid in an address, not in a token. */
        const val SEPARATOR = "\u001F"
    }
}
