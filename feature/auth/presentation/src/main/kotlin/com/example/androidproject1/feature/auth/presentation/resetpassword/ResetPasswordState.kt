package com.example.androidproject1.feature.auth.presentation.resetpassword

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.auth.presentation.signup.SignUpState
import com.example.androidproject1.service.core.ui.form.FieldState
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.matches
import com.example.androidproject1.service.core.ui.form.minLength
import com.example.androidproject1.service.core.ui.form.required
import com.example.androidproject1.service.core.ui.text.UiText

/** SignUp's password pair under the same rules, and the link's token it will be sent with. */
@Immutable
data class ResetPasswordState(
    val token: String,
    val password: FieldState = FieldState.of(required(), minLength(SignUpState.MIN_PASSWORD_LENGTH)),
    val confirmPassword: FieldState = FieldState.of(required()),
) {

    /** The confirm field's own rule, then the mismatch once touched — as on SignUp, and for its reason. */
    val confirmPasswordError: UiText?
        get() = confirmPassword.error
            ?: matches(other = { password.value })(confirmPassword.value).takeIf { confirmPassword.touched }

    val canSubmit: Boolean get() = Form.canSubmit(password, confirmPassword) && password.value == confirmPassword.value

    val firstError: UiText? get() = Form.firstError(password) ?: confirmPasswordError

    val isDirty: Boolean get() = password.value.isNotEmpty() || confirmPassword.value.isNotEmpty()

    companion object {

        val PREVIEW = ResetPasswordState(
            token = "fixture",
            password = FieldState.of(required(), minLength(SignUpState.MIN_PASSWORD_LENGTH), value = "correct horse"),
            confirmPassword = FieldState.of(required(), value = "correct horse"),
        )

        /** What the confirm field says once the two differ. */
        val PREVIEW_MISMATCH = PREVIEW.copy(confirmPassword = PREVIEW.confirmPassword.changed("correct horsf"))
    }
}
