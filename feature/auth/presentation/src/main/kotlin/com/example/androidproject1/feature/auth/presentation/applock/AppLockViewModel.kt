package com.example.androidproject1.feature.auth.presentation.applock

import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.auth.domain.AppLock
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.service.biometric.BiometricGate
import com.example.androidproject1.service.biometric.GateResult
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.launch

/**
 * The app lock's screen: it asks as it appears, and again on Unlock. A proof lets the user
 * in; backing out or a refusal keeps it locked; a device with nothing left to prove it with — the
 * screen lock removed since the switch went on — lets them in, because the gate never blocks what
 * it cannot ask.
 */
class AppLockViewModel(
    logger: Logger,
    private val appLock: AppLock,
    private val biometric: BiometricGate,
) : BaseViewModel<AppLockState, AppLockEvent, AppLockNavigation>(
    initialState = AppLockState.PREVIEW,
    logger = logger.withTag("AppLockViewModel"),
) {

    init {
        ask()
    }

    override fun onUiEvent(event: AppLockEvent) = when (event) {
        AppLockEvent.UnlockClicked -> ask()
    }

    private fun ask() {
        viewModelScope.launch {
            updateData { copy(asking = true) }
            when (biometric.confirm(R.string.app_lock_prompt.toUiText())) {
                GateResult.Confirmed, GateResult.Unavailable -> appLock.unlocked()
                GateResult.Cancelled, GateResult.Denied -> Unit
            }
            updateData { copy(asking = false) }
        }
    }
}
