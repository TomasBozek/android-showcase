package com.example.androidproject1.feature.auth.presentation.signup

import androidx.lifecycle.SavedStateHandle
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.domain.TermsPolicy
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.form.ALERT_ID_DISCARD
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.discardAlert
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger

class SignUpViewModel(
    logger: Logger,
    private val authService: AuthService,
    private val termsPolicy: TermsPolicy,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<SignUpState, SignUpEvent, SignUpNavigation>(
    initialState = SignUpState(),
    logger = logger.withTag("SignUpViewModel"),
    savedStateHandle = savedStateHandle,
) {

    // The address and the box survive process death; the passwords never do — saved state is on disk.
    private var savedEmail: String by saved(KEY_EMAIL, "")
    private var savedTermsAccepted: Boolean by saved(KEY_TERMS_ACCEPTED, false)

    init {
        if (savedEmail.isNotEmpty()) updateData { copy(email = email.changed(savedEmail)) }
        if (savedTermsAccepted) updateData { copy(termsAccepted = true) }
    }

    override fun onUiEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.EmailChanged -> {
                savedEmail = event.email
                updateData { copy(email = email.changed(event.email)) }
            }

            is SignUpEvent.PasswordChanged ->
                updateData { copy(password = password.changed(event.password)) }

            is SignUpEvent.ConfirmPasswordChanged ->
                updateData {
                    copy(confirmPassword = confirmPassword.changed(event.confirmPassword))
                }

            SignUpEvent.SignUpClicked -> submit()

            is SignUpEvent.TermsToggled -> {
                savedTermsAccepted = event.accepted
                updateData { copy(termsAccepted = event.accepted) }
            }

            // A Custom Tab over the form, so what was typed is still there on the way back.
            SignUpEvent.TermsLinkClicked -> sendCommand(UiCommand.OpenBrowser(termsPolicy.termsUrl))

            SignUpEvent.PrivacyLinkClicked -> sendCommand(UiCommand.OpenBrowser(termsPolicy.privacyUrl))

            SignUpEvent.LoginClicked -> navigate(SignUpNavigation.Login)

            SignUpEvent.BackRequested -> uiState.setAlert(discardAlert())
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_DISCARD) {
            uiState.clearAlert()
            navigate(SignUpNavigation.Login)
            return
        }
        super.onSystemEvent(event)
    }

    /** Create the account, or say why not — the same rule as Login's submit. */
    private fun submit() {
        val state = uiState.value.data ?: return
        if (!state.canSubmit) {
            val (email, password, confirmPassword) = Form.touchAll(state.email, state.password, state.confirmPassword)
            val revealed = state.copy(
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                termsTouched = true,
            )
            updateData { revealed }
            revealed.firstError?.let { showSnackbar(it) }
            return
        }
        signUp(email = state.email.value)
    }

    // The account and a mailed code — no session yet; that starts once the code is in.
    private fun signUp(email: String) = execute(
        loading = overlay(),
        action = { authService.signUp(email) },
        onData = { navigate(SignUpNavigation.VerifyEmail(email.trim())) },
    )

    private companion object {

        const val KEY_EMAIL = "sign_up_email"
        const val KEY_TERMS_ACCEPTED = "sign_up_terms_accepted"
    }
}
