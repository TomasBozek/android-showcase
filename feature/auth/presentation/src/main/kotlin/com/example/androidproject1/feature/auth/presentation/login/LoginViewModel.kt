package com.example.androidproject1.feature.auth.presentation.login

import androidx.lifecycle.SavedStateHandle
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.form.ALERT_ID_DISCARD
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.discardAlert
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger

class LoginViewModel(
    logger: Logger,
    private val authService: AuthService,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LoginState, LoginEvent, LoginNavigation>(
    initialState = LoginState(),
    logger = logger.withTag("LoginViewModel"),
    savedStateHandle = savedStateHandle,
) {

    // The address survives process death; the password never does — saved state is on disk.
    private var savedEmail: String by saved(KEY_EMAIL, "")

    init {
        if (savedEmail.isNotEmpty()) updateData { copy(email = email.changed(savedEmail)) }
    }

    override fun onUiEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                savedEmail = event.email
                updateData { copy(email = email.changed(event.email)) }
            }

            is LoginEvent.PasswordChanged ->
                updateData { copy(password = password.changed(event.password)) }

            LoginEvent.LoginClicked -> submit()

            LoginEvent.SkipLoginClicked -> login(email = "")

            LoginEvent.SignUpClicked -> navigate(LoginNavigation.SignUp)

            LoginEvent.ForgotPasswordClicked -> navigate(LoginNavigation.ForgotPassword)

            LoginEvent.BackRequested -> uiState.setAlert(discardAlert())
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_DISCARD) {
            uiState.clearAlert()
            // Login is the root of the auth flow, so leaving it leaves the app.
            sendCommand(UiCommand.CloseApp)
            return
        }
        super.onSystemEvent(event)
    }

    /** A new password was set through a reset link; the one who set it is here to use it. */
    fun onPasswordReset() = showSnackbar(R.string.login_password_reset.toUiText())

    /**
     * Sign in, or say why not.
     *
     * The button stays enabled on an invalid form on purpose: a disabled submit can say
     * *whether* the form is submittable and never *why*. Pressing it touches every field, which is
     * what reveals the errors, and the first one is repeated in a snackbar for anyone who is
     * looking at the button rather than the field.
     */
    private fun submit() {
        val state = uiState.value.data ?: return
        if (!state.canSubmit) {
            val (email, password) = Form.touchAll(state.email, state.password)
            val revealed = state.copy(email = email, password = password)
            updateData { revealed }
            revealed.firstError?.let { showSnackbar(it) }
            return
        }
        login(email = state.email.value)
    }

    private fun login(email: String) = execute(
        loading = overlay(),
        action = { authService.login(email) },
        onData = { logger.d { "Signed in" } },
    )

    private companion object {

        const val KEY_EMAIL = "login_email"
    }
}
