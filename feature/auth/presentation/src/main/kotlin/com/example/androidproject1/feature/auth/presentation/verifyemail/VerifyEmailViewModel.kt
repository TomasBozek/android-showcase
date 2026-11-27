package com.example.androidproject1.feature.auth.presentation.verifyemail

import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.auth.api.VerifyEmailDestination
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.feature.auth.presentation.ResendCountdown
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger

/**
 * The code sign-up mailed, and the session it starts. A right code signs in, and the
 * session change moves the app to the main flow by itself; a wrong one is the server's `400`,
 * which the alert reports.
 */
class VerifyEmailViewModel(
    logger: Logger,
    private val args: VerifyEmailDestination,
    private val authService: AuthService,
) : BaseViewModel<VerifyEmailState, VerifyEmailEvent, VerifyEmailNavigation>(
    initialState = VerifyEmailState(email = args.email),
    logger = logger.withTag("VerifyEmailViewModel"),
) {

    private val countdown = ResendCountdown(viewModelScope) { left -> updateData { copy(resendInSeconds = left) } }

    init {
        // Sign-up has just mailed the first code, so the wait starts with the screen.
        countdown.start()
    }

    override fun onUiEvent(event: VerifyEmailEvent) {
        when (event) {
            is VerifyEmailEvent.CodeChanged -> updateData { copy(code = event.code, codeError = null) }

            VerifyEmailEvent.SubmitClicked -> submit()

            VerifyEmailEvent.ResendClicked -> resend()

            VerifyEmailEvent.NavigateUpClicked -> navigate(VerifyEmailNavigation.NavigateUp)
        }
    }

    /** Login's policy: the key is always enabled, and a code that is not complete says why. */
    private fun submit() {
        val state = uiState.value.data ?: return
        if (!state.isComplete) {
            val error = R.string.verify_email_code_incomplete.toUiText()
            updateData { copy(codeError = error) }
            showSnackbar(error)
            return
        }
        execute(
            loading = overlay(),
            action = { authService.verifyEmail(email = args.email, code = state.code) },
            onData = { logger.d { "Verified" } },
        )
    }

    private fun resend() {
        val state = uiState.value.data ?: return
        if (state.resendInSeconds > 0) return
        execute(
            loading = overlay(),
            action = { authService.resendVerification(args.email) },
            onData = {
                updateData { copy(code = "", codeError = null) }
                countdown.start()
            },
        )
    }
}
