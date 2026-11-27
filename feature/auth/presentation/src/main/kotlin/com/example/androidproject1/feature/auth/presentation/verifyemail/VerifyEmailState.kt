package com.example.androidproject1.feature.auth.presentation.verifyemail

import androidx.compose.runtime.Immutable
import com.example.androidproject1.core.ui.component.field.DEFAULT_CODE_LENGTH
import com.example.androidproject1.service.core.ui.text.UiText

@Immutable
data class VerifyEmailState(
    /** Where the code went. The route's, and what the screen says it was sent to. */
    val email: String,
    val code: String = "",
    /** Set when submit is pressed on a code that is not six digits yet. */
    val codeError: UiText? = null,
    /** Seconds until another code may be asked for; `0` once it may. */
    val resendInSeconds: Int = 0,
) {

    val isComplete: Boolean get() = code.length == DEFAULT_CODE_LENGTH

    companion object {

        val PREVIEW = VerifyEmailState(email = "ada@example.com", code = "4209", resendInSeconds = 18)
    }
}
