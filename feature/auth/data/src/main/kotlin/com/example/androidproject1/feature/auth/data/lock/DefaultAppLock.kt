package com.example.androidproject1.feature.auth.data.lock

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.androidproject1.feature.auth.domain.AppLock
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.lifecycle.Foreground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * [AppLock] over a DataStore preference and the process's [Foreground].
 *
 * A cold start is held — the object is created once per process — and so is a return after
 * [AWAY_LIMIT] or more in the background; [unlocked] lets the user in until the next of either.
 * Switching the lock on does not hold the user who just switched it: they are already in.
 *
 * @param scope where the rule runs for the life of the process — `:feature:auth:di` passes one of
 * its own, a test its `backgroundScope`.
 */
class DefaultAppLock(
    dataStoreProvider: DataStoreProvider,
    foreground: Foreground,
    private val clock: Clock,
    scope: CoroutineScope,
) : AppLock {

    private val dataStore = dataStoreProvider.dataStore

    /** Held until unlocked: true at a cold start. */
    private var held = true

    /** The switch as last read; `null` until DataStore has answered once. */
    private var on: Boolean? = null

    private var leftAt: Instant? = null

    // Written in place rather than combined from two flows, so a read right after `unlocked()` or a
    // return from the background already sees it — the lock screen reads it the same way.
    private val state = MutableStateFlow<Boolean?>(null)

    override val enabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_ENABLED] ?: false }.distinctUntilChanged()

    override val locked: StateFlow<Boolean?> = state.asStateFlow()

    init {
        enabled.onEach {
            on = it
            publish()
        }.launchIn(scope)
        foreground.isForeground
            .onEach { inFront ->
                if (!inFront) {
                    leftAt = clock.instant()
                } else {
                    val left = leftAt
                    leftAt = null
                    if (left != null && Duration.between(left, clock.instant()) >= AWAY_LIMIT) {
                        held = true
                        publish()
                    }
                }
            }
            .launchIn(scope)
    }

    override suspend fun setEnabled(enabled: Boolean) {
        // Whoever flips the switch is already in; turning it off lets in an app that is held.
        held = false
        on = enabled
        publish()
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    override fun unlocked() {
        held = false
        publish()
    }

    private fun publish() {
        state.value = on?.let { it && held }
    }

    companion object {

        /** How long away holds the app again. */
        val AWAY_LIMIT: Duration = Duration.ofMinutes(5)

        private val KEY_ENABLED = booleanPreferencesKey("auth_app_lock_enabled")
    }
}
