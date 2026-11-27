package com.example.androidproject1.feature.settings.data.source

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** The count and the flag, in the app's one preferences file. Per install, so a reinstall asks again. */
class DefaultLocalReviewDataSource(
    dataStoreProvider: DataStoreProvider,
    private val dispatcherProvider: DispatcherProvider,
) : LocalReviewDataSource {

    private val dataStore = dataStoreProvider.dataStore

    override suspend fun addMilestone(): Int = withContext(dispatcherProvider.io) {
        var count = 0
        // One edit, so two saves racing each other are both counted.
        dataStore.edit { preferences ->
            count = (preferences[KEY_MILESTONES] ?: 0) + 1
            preferences[KEY_MILESTONES] = count
        }
        count
    }

    override suspend fun hasAsked(): Boolean = withContext(dispatcherProvider.io) {
        dataStore.data.first()[KEY_ASKED] ?: false
    }

    override suspend fun markAsked() = withContext(dispatcherProvider.io) {
        dataStore.edit { preferences -> preferences[KEY_ASKED] = true }
        Unit
    }

    private companion object {

        val KEY_MILESTONES = intPreferencesKey("settings_review_milestones")
        val KEY_ASKED = booleanPreferencesKey("settings_review_asked")
    }
}
