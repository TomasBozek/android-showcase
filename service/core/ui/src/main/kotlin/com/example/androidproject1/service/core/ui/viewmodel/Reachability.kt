package com.example.androidproject1.service.core.ui.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Whether the device has a route out, as [BaseViewModel] sees it: an offline failure waits
 * here for the network to come back.
 *
 * A port rather than `ConnectivityMonitor` itself, which is `:service:network`'s and would drag the
 * HTTP client into this module; `:app` installs the monitor's `online` once, at start. It is the
 * process's rather than a constructor parameter so no view model has to ask for it. Until something
 * is installed it says online, which is how every screen behaved before it existed.
 */
public object Reachability {

    private val always: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    private val source = MutableStateFlow(always)

    /** Whether a request has somewhere to go right now. */
    public val isOnline: Boolean
        get() = source.value.value

    /** Suspends until the device is online — at once if it already is — following a reinstall. */
    @OptIn(ExperimentalCoroutinesApi::class)
    public suspend fun awaitOnline() {
        source.flatMapLatest { it }.first { it }
    }

    public fun install(online: StateFlow<Boolean>) {
        source.value = online
    }

    /** Back to always-online — for a test, after it scripted the network. */
    public fun reset() {
        source.value = always
    }
}
