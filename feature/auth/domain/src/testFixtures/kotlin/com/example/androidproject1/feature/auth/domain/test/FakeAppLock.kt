package com.example.androidproject1.feature.auth.domain.test

import com.example.androidproject1.feature.auth.domain.AppLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** An [AppLock] a test scripts: set [isEnabled] and [held] directly, and count the unlocks. */
class FakeAppLock(enabled: Boolean = false, held: Boolean? = false) : AppLock {

    val isEnabled = MutableStateFlow(enabled)
    val held = MutableStateFlow(held)
    var unlockCount = 0
        private set

    override val enabled: Flow<Boolean> = isEnabled

    override suspend fun setEnabled(enabled: Boolean) {
        isEnabled.value = enabled
    }

    override val locked: StateFlow<Boolean?> = this.held

    override fun unlocked() {
        unlockCount++
        held.value = false
    }
}
