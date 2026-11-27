package com.example.androidproject1.feature.auth.presentation.forgotpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.feature.auth.presentation.ResendCountdown
import com.example.androidproject1.service.core.domain.error.RateLimitedError
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.form.ALERT_ID_DISCARD
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.discardAlert
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import java.time.Clock

/**
 * Asks for a reset link, then counts down to when another may be asked for.
 *
 * The address, whether it was sent and when resend opens are [saved], so a screen brought back
 * after process death is still on "check your inbox" with the time that was left, rather than an
 * empty form inviting a second request the server would refuse.
 */
class ForgotPasswordViewModel(
    logger: Logger,
    private val authService: AuthService,
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
) : BaseViewModel<ForgotPasswordState, ForgotPasswordEvent, ForgotPasswordNavigation>(
    initialState = ForgotPasswordState(),
    logger = logger.withTag("ForgotPasswordViewModel"),
    savedStateHandle = savedStateHandle,
) {

    private var savedEmail: String by saved(KEY_EMAIL, "")
    private var sentAt: Long by saved(KEY_SENT_AT, 0L)
    private val countdown = ResendCountdown(viewModelScope) { left -> updateData { copy(resendInSeconds = left) } }

    init {
        if (savedEmail.isNotEmpty()) updateData { copy(email = email.changed(savedEmail)) }
        if (sentAt > 0L) {
            val left = ForgotPasswordState.RESEND_AFTER_SECONDS - ((clock.millis() - sentAt) / MILLIS).toInt()
            updateData { copy(sent = true) }
            countdown.start(from = left.coerceIn(0, ForgotPasswordState.RESEND_AFTER_SECONDS))
        }
    }

    override fun onUiEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.EmailChanged -> {
                savedEmail = event.email
                updateData { copy(email = email.changed(event.email)) }
            }

            ForgotPasswordEvent.SubmitClicked -> submit()

            ForgotPasswordEvent.ResendClicked -> {
                val state = uiState.value.data ?: return
                if (state.sent && state.resendInSeconds == 0) request(state.email.value)
            }

            ForgotPasswordEvent.NavigateUpClicked -> navigate(ForgotPasswordNavigation.NavigateUp)

            ForgotPasswordEvent.BackRequested -> uiState.setAlert(discardAlert())
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_DISCARD) {
            uiState.clearAlert()
            navigate(ForgotPasswordNavigation.NavigateUp)
            return
        }
        super.onSystemEvent(event)
    }

    /** Login's policy: the key is always enabled, and pressing it on a bad form says why. */
    private fun submit() {
        val state = uiState.value.data ?: return
        if (!Form.canSubmit(state.email)) {
            val (email) = Form.touchAll(state.email)
            updateData { copy(email = email) }
            Form.firstError(email)?.let { showSnackbar(it) }
            return
        }
        request(state.email.value)
    }

    private fun request(address: String) = execute(
        loading = overlay(),
        action = { authService.requestPasswordReset(address.trim()) },
        // A 429: the server is fine and so is the address, it is just too soon to ask again.
        onError = { error ->
            (error is RateLimitedError).also { limited ->
                if (limited) showSnackbar(R.string.forgot_password_rate_limited.toUiText())
            }
        },
        onData = {
            sentAt = clock.millis()
            updateData { copy(sent = true) }
            countdown.start()
        },
    )

    private companion object {

        const val KEY_EMAIL = "forgot_password_email"
        const val KEY_SENT_AT = "forgot_password_sent_at"
        const val MILLIS = 1_000L
    }
}
