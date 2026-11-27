package com.example.androidproject1.config

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/** The startup config, asked for once per start and failing open. */
interface AppConfigRepository {

    /** The server's last answer this install knows; `null` when it has never heard one. */
    val config: StateFlow<AppConfig?>

    /**
     * Asks `GET config`. An answer replaces what was kept; no answer leaves the kept one in force,
     * read from the cache on the first call — never a failure, since a config that cannot be read
     * holds nothing.
     */
    suspend fun refresh()

    /** The newest version whose update offer was dismissed on this install; `0` for none. */
    suspend fun dismissedUpdate(): Int

    /** Puts the offer of [versionCode] away for good; a newer one is offered again. */
    suspend fun dismissUpdate(versionCode: Int)
}

class DefaultAppConfigRepository(
    private val client: HttpClient,
    dataStoreProvider: DataStoreProvider,
    private val dispatcherProvider: DispatcherProvider,
) : AppConfigRepository {

    private val dataStore = dataStoreProvider.dataStore
    private val mutableConfig = MutableStateFlow<AppConfig?>(null)

    override val config: StateFlow<AppConfig?> = mutableConfig.asStateFlow()

    override suspend fun refresh() = withContext(dispatcherProvider.io) {
        if (mutableConfig.value == null) mutableConfig.value = cached()
        val answer = try {
            client.get("config").body<AppConfig>()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Offline, a 5xx, a body that does not parse: all the same here — what was kept stands.
            null
        }
        if (answer != null) {
            dataStore.edit { it[KEY_CONFIG] = json.encodeToString(AppConfig.serializer(), answer) }
            mutableConfig.value = answer
        }
    }

    override suspend fun dismissedUpdate(): Int = withContext(dispatcherProvider.io) {
        dataStore.data.first()[KEY_DISMISSED_UPDATE] ?: 0
    }

    override suspend fun dismissUpdate(versionCode: Int) = withContext(dispatcherProvider.io) {
        dataStore.edit { it[KEY_DISMISSED_UPDATE] = versionCode }
        Unit
    }

    private suspend fun cached(): AppConfig? = dataStore.data.first()[KEY_CONFIG]?.let {
        runCatching { json.decodeFromString(AppConfig.serializer(), it) }.getOrNull()
    }

    private companion object {

        val KEY_CONFIG = stringPreferencesKey("app_config")
        val KEY_DISMISSED_UPDATE = intPreferencesKey("app_config_dismissed_update")
        val json = Json { ignoreUnknownKeys = true }
    }
}
