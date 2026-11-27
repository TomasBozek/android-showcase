package com.example.androidproject1.feature.auth.presentation.forgotpassword

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.auth.presentation.ResendCountdown
import com.example.androidproject1.service.core.ui.form.FieldState
import com.example.androidproject1.service.core.ui.form.email
import com.example.androidproject1.service.core.ui.form.required

@Immutable
data class ForgotPasswordState(
    val email: FieldState = FieldState.of(required(), email()),
    /** The request went out: the form gives way to "check your inbox". */
    val sent: Boolean = false,
    /** Seconds until resend is offered; `0` once it is. Meaningless while [sent] is false. */
    val resendInSeconds: Int = 0,
) {

    /** Something typed and not yet sent. Back is guarded then, as on every form. */
    val isDirty: Boolean get() = !sent && email.value.isNotEmpty()

    companion object {

        /** How long before another link may be asked for — the shared countdown's. */
        const val RESEND_AFTER_SECONDS = ResendCountdown.SECONDS

        val EMPTY = ForgotPasswordState()

        val PREVIEW = ForgotPasswordState(email = FieldState.of(required(), email(), value = "ada@example.com"))

        val PREVIEW_SENT = PREVIEW.copy(sent = true, resendInSeconds = 24)
    }
}
