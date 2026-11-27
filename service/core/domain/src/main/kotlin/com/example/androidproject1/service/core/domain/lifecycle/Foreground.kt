package com.example.androidproject1.service.core.domain.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the app is in front of the user: `true` while any of its activities is started,
 * `false` from the moment the last one stops. `:app` binds it over `ProcessLifecycleOwner`, so it
 * is the process's answer — a rotation is not a trip to the background.
 *
 * A port in the domain so a view model can react to a return without naming the platform.
 */
public interface Foreground {

    public val isForeground: StateFlow<Boolean>

    public companion object {

        /** Always in front — the default in a test and wherever no lifecycle is bound. */
        public val Always: Foreground = object : Foreground {
            override val isForeground: StateFlow<Boolean> = MutableStateFlow(true)
        }
    }
}
