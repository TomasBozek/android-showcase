package com.example.androidproject1.feature.auth.presentation.resetpassword

import com.example.androidproject1.feature.auth.api.ResetPasswordDestination
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.form.ALERT_ID_DISCARD
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.discardAlert
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger

/**
 * Where a reset link lands. The token is the route's; nothing is loaded — a token is
 * checked when it is used, so an expired one is found out at submit, with the alert that says so.
 */
class ResetPasswordViewModel(
    logger: Logger,
    private val args: ResetPasswordDestination,
    private val authService: AuthService,
) : BaseViewModel<ResetPasswordState, ResetPasswordEvent, ResetPasswordNavigation>(
    initialState = ResetPasswordState(token = args.token),
    logger = logger.withTag("ResetPasswordViewModel"),
) {

    override fun onUiEvent(event: ResetPasswordEvent) {
        when (event) {
            is ResetPasswordEvent.PasswordChanged -> updateData { copy(password = password.changed(event.password)) }

            is ResetPasswordEvent.ConfirmPasswordChanged ->
                updateData { copy(confirmPassword = confirmPassword.changed(event.confirmPassword)) }

            ResetPasswordEvent.SubmitClicked -> submit()

            ResetPasswordEvent.NavigateUpClicked -> navigate(ResetPasswordNavigation.NavigateUp)

            ResetPasswordEvent.BackRequested -> uiState.setAlert(discardAlert())
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_DISCARD) {
            uiState.clearAlert()
            navigate(ResetPasswordNavigation.NavigateUp)
            return
        }
        super.onSystemEvent(event)
    }

    /** SignUp's policy: the key is always enabled, and pressing it on a bad pair says why. */
    private fun submit() {
        val state = uiState.value.data ?: return
        if (!state.canSubmit) {
            val (password, confirm) = Form.touchAll(state.password, state.confirmPassword)
            val revealed = state.copy(password = password, confirmPassword = confirm)
            updateData { revealed }
            revealed.firstError?.let { showSnackbar(it) }
            return
        }
        execute(
            loading = overlay(),
            action = { authService.resetPassword(token = args.token, password = state.password.value) },
            onData = { navigate(ResetPasswordNavigation.Done) },
        )
    }
}
