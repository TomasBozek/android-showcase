package com.example.androidproject1.feature.profile.data.source

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidproject1.feature.profile.domain.Profile
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
class DefaultLocalProfileDataSource(
    dataStoreProvider: DataStoreProvider,
    private val dispatcherProvider: DispatcherProvider,
) : LocalProfileDataSource {

    private val dataStore = dataStoreProvider.dataStore

    override suspend fun read(): Profile = withContext(dispatcherProvider.io) {
        dataStore.data.first().toProfile()
    }

    override fun observe(): Flow<Profile> =
        dataStore.data
            .map { preferences -> preferences.toProfile() }
            // Every feature writes into the same preferences file and DataStore re-emits all of
            // it on any edit; without this, a theme change would re-emit an unchanged profile.
            .distinctUntilChanged()
            .flowOn(dispatcherProvider.io)

    private fun Preferences.toProfile() = Profile(
        name = this[KEY_NAME].orEmpty(),
        email = this[KEY_EMAIL].orEmpty(),
        avatarUri = this[KEY_AVATAR_URI],
    )

    override suspend fun writeDetails(name: String, email: String) {
        withContext(dispatcherProvider.io) {
            dataStore.edit { preferences ->
                preferences[KEY_NAME] = name
                preferences[KEY_EMAIL] = email
            }
        }
    }

    override suspend fun writeAvatarUri(uri: String) {
        withContext(dispatcherProvider.io) {
            dataStore.edit { preferences -> preferences[KEY_AVATAR_URI] = uri }
        }
    }

    override suspend fun clear() {
        withContext(dispatcherProvider.io) {
            dataStore.edit { preferences ->
                preferences.remove(KEY_NAME)
                preferences.remove(KEY_EMAIL)
                preferences.remove(KEY_AVATAR_URI)
            }
        }
    }

    private companion object {

        val KEY_NAME = stringPreferencesKey("profile_name")
        val KEY_EMAIL = stringPreferencesKey("profile_email")
        val KEY_AVATAR_URI = stringPreferencesKey("profile_avatar_uri")
    }
}
