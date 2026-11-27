package com.example.androidproject1.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.androidproject1.service.core.domain.lifecycle.Foreground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [Foreground] over [ProcessLifecycleOwner]: started while any activity is, stopped a
 * moment after the last one stops — the library waits out a rotation, so it is not a trip away.
 *
 * Must be made on the main thread, where the observer is added; `ApplicationModule` makes it at
 * start, which is.
 */
class ProcessForeground(owner: LifecycleOwner = ProcessLifecycleOwner.get()) :
    Foreground,
    DefaultLifecycleObserver {

    private val state = MutableStateFlow(false)

    override val isForeground: StateFlow<Boolean> = state.asStateFlow()

    init {
        owner.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        state.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        state.value = false
    }
}
