package com.example.androidproject1.feature.onboarding.data.source

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Switching to IO is this class's job, not the repository's: `BaseRepository` runs on the caller's
 * context and the caller is `viewModelScope`, which is `Dispatchers.Main`.
 */
class DefaultLocalOnboardingDataSource(
    dataStoreProvider: DataStoreProvider,
    private val dispatcherProvider: DispatcherProvider,
) : LocalOnboardingDataSource {

    private val dataStore = dataStoreProvider.dataStore

    override fun observeSeen(): Flow<Boolean> =
        dataStore.data
            .map { preferences -> preferences[KEY_SEEN] == true }
            // Every feature writes into the same preferences file and DataStore re-emits all of it
            // on any edit; without this, saving a profile would re-decide which flow the app is in.
            .distinctUntilChanged()
            .flowOn(dispatcherProvider.io)

    override suspend fun setSeen(seen: Boolean) = withContext(dispatcherProvider.io) {
        dataStore.edit { preferences -> preferences[KEY_SEEN] = seen }
        Unit
    }

    override fun observeTourSeen(): Flow<Boolean> =
        dataStore.data
            .map { preferences -> preferences[KEY_TOUR_SEEN] == true }
            .distinctUntilChanged()
            .flowOn(dispatcherProvider.io)

    override suspend fun setTourSeen(seen: Boolean) = withContext(dispatcherProvider.io) {
        dataStore.edit { preferences -> preferences[KEY_TOUR_SEEN] = seen }
        Unit
    }

    override suspend fun lastSeenVersionCode(): Int = withContext(dispatcherProvider.io) {
        dataStore.data.first()[KEY_LAST_SEEN_VERSION_CODE] ?: 0
    }

    override suspend fun setLastSeenVersionCode(versionCode: Int) = withContext(dispatcherProvider.io) {
        dataStore.edit { preferences -> preferences[KEY_LAST_SEEN_VERSION_CODE] = versionCode }
        Unit
    }

    private companion object {

        val KEY_SEEN = booleanPreferencesKey("onboarding_seen")
        val KEY_TOUR_SEEN = booleanPreferencesKey("tour_seen")
        val KEY_LAST_SEEN_VERSION_CODE = intPreferencesKey("whats_new_last_seen_version_code")
    }
}
