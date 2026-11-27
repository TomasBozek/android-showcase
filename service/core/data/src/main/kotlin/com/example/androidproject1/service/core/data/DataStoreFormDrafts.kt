package com.example.androidproject1.service.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidproject1.service.core.domain.FormDrafts
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * [FormDrafts] in the app's preferences store, each draft one string under a prefixed key.
 * DataStore applies edits in the order they arrive, so a form that writes on every keystroke ends
 * on its last one. [erase] removes every draft and nothing else in the file.
 */
public class DataStoreFormDrafts(
    dataStoreProvider: DataStoreProvider,
    private val dispatcherProvider: DispatcherProvider,
) : FormDrafts, LocalDataEraser {

    private val dataStore = dataStoreProvider.dataStore

    override suspend fun read(key: String): String? = withContext(dispatcherProvider.io) {
        dataStore.data.first()[preferenceKey(key)]
    }

    override suspend fun write(key: String, value: String) {
        withContext(dispatcherProvider.io) { dataStore.edit { it[preferenceKey(key)] = value } }
    }

    override suspend fun clear(key: String) {
        withContext(dispatcherProvider.io) { dataStore.edit { it.remove(preferenceKey(key)) } }
    }

    override suspend fun erase() {
        withContext(dispatcherProvider.io) {
            dataStore.edit { preferences ->
                preferences.asMap().keys.filter { it.name.startsWith(PREFIX) }.forEach { preferences.remove(it) }
            }
        }
    }

    private fun preferenceKey(key: String) = stringPreferencesKey(PREFIX + key)

    private companion object {

        const val PREFIX = "form_draft_"
    }
}
