package com.example.androidproject1.message

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.network.HttpErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** The server's messages, and which of them this install has dismissed. */
interface MessageRepository {

    /** `GET messages`. Throws a `DomainError`; the caller decides that a failure means none. */
    suspend fun messages(): List<ServerMessage>

    suspend fun dismissed(): Set<String>

    suspend fun dismiss(id: String)

    /** Forgets every dismissal, so the messages show again — the debug menu's reset. */
    suspend fun resetDismissed()
}

class DefaultMessageRepository(
    private val client: HttpClient,
    dataStoreProvider: DataStoreProvider,
    private val dispatcherProvider: DispatcherProvider,
) : MessageRepository {

    private val dataStore = dataStoreProvider.dataStore

    override suspend fun messages(): List<ServerMessage> = withContext(dispatcherProvider.io) {
        try {
            client.get("messages").body<List<ServerMessage>>()
        } catch (throwable: Throwable) {
            throw HttpErrorMapper.map(throwable)
        }
    }

    override suspend fun dismissed(): Set<String> = withContext(dispatcherProvider.io) {
        dataStore.data.first()[KEY_DISMISSED].orEmpty()
    }

    override suspend fun dismiss(id: String) = withContext(dispatcherProvider.io) {
        dataStore.edit { it[KEY_DISMISSED] = it[KEY_DISMISSED].orEmpty() + id }
        Unit
    }

    override suspend fun resetDismissed() = withContext(dispatcherProvider.io) {
        dataStore.edit { it.remove(KEY_DISMISSED) }
        Unit
    }

    private companion object {

        val KEY_DISMISSED = stringSetPreferencesKey("app_messages_dismissed")
    }
}
