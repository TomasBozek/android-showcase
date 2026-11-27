package com.example.androidproject1.service.telemetry

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidproject1.service.core.domain.LocalDataEraser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * The one identity telemetry attaches: a random UUID for this install, never a hardware id.
 *
 * It names an install, not a person or a device — reinstalling, or deleting the account's data,
 * gives a new one, so nothing can follow the user across either.
 */
public interface InstallId {

    /** This install's id, minted on the first ask and the same on every ask after. */
    public suspend fun get(): String
}

/**
 * [InstallId] kept in the app's preferences [store], and a [LocalDataEraser] so "delete my
 * account" takes it with the rest: the next ask mints a new one. A sign-out leaves it: it names
 * the install, not whoever was signed in.
 *
 * @param mint what makes a new id; a test counts the calls.
 */
public class DataStoreInstallId(
    private val store: DataStore<Preferences>,
    private val mint: () -> String = { UUID.randomUUID().toString() },
) : InstallId,
    LocalDataEraser {

    // Two first asks at once must not mint two ids, the second overwriting the one the first returned.
    private val lock = Mutex()

    override suspend fun get(): String = lock.withLock {
        store.data.first()[KEY] ?: mint().also { id -> store.edit { it[KEY] = id } }
    }

    override val erasesOnSignOut: Boolean = false

    override suspend fun erase() {
        lock.withLock { store.edit { it.remove(KEY) } }
    }

    private companion object {
        val KEY = stringPreferencesKey("telemetry_install_id")
    }
}
