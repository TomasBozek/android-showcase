package com.example.androidproject1.feature.checkout.data.source

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * One JSON value in the app's preferences store, beside the profile's keys: a draft is one
 * small document read once per launch, which is what a key-value store is for. A value this build
 * cannot read is no draft rather than a crash.
 *
 * Switching to IO is this class's job, not the repository's: `BaseRepository` runs on the caller's
 * context and the caller is `viewModelScope`, which is `Dispatchers.Main`.
 */
class DefaultLocalCheckoutDraftDataSource(
    dataStoreProvider: DataStoreProvider,
    private val dispatcherProvider: DispatcherProvider,
) : LocalCheckoutDraftDataSource {

    private val dataStore = dataStoreProvider.dataStore

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(): StoredCheckoutDraft? = withContext(dispatcherProvider.io) {
        dataStore.data.first()[KEY_DRAFT]?.let { stored ->
            runCatching { json.decodeFromString(StoredCheckoutDraft.serializer(), stored) }.getOrNull()
        }
    }

    override suspend fun write(draft: StoredCheckoutDraft?) = withContext(dispatcherProvider.io) {
        dataStore.edit { preferences ->
            if (draft == null) {
                preferences.remove(KEY_DRAFT)
            } else {
                preferences[KEY_DRAFT] = json.encodeToString(StoredCheckoutDraft.serializer(), draft)
            }
        }
        Unit
    }

    private companion object {

        val KEY_DRAFT = stringPreferencesKey("checkout_draft")
    }
}
