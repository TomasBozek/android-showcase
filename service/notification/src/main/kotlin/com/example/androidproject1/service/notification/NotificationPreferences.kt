package com.example.androidproject1.service.notification

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.androidproject1.service.core.data.DataStoreProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * The app's own switch for each [NotificationKind], apart from the system's: what the user turned
 * off in the app's settings is dropped before anything is posted. Every kind is on until it is
 * switched off.
 */
public interface NotificationPreferences {

    public fun observe(kind: NotificationKind): Flow<Boolean>

    /** The current answer, for [Notifier.post], which is not a suspend function. */
    public fun isOn(kind: NotificationKind): Boolean

    public suspend fun set(kind: NotificationKind, on: Boolean)
}

/**
 * [NotificationPreferences] in the app's preferences file. [isOn] reads the store on the calling
 * thread and blocks for it — a post comes from a worker or a tap, and the store is a small file the
 * process already holds open.
 */
public class DataStoreNotificationPreferences(dataStoreProvider: DataStoreProvider) : NotificationPreferences {

    private val dataStore = dataStoreProvider.dataStore

    private fun key(kind: NotificationKind) = booleanPreferencesKey("notifications_${kind.name.lowercase()}_on")

    override fun observe(kind: NotificationKind): Flow<Boolean> =
        dataStore.data.map { it[key(kind)] ?: true }.distinctUntilChanged()

    override fun isOn(kind: NotificationKind): Boolean = runBlocking { observe(kind).first() }

    override suspend fun set(kind: NotificationKind, on: Boolean) {
        withContext(Dispatchers.IO) { dataStore.edit { it[key(kind)] = on } }
    }
}
