package com.example.androidproject1.feature.auth.presentation.signup

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.service.core.ui.form.FieldState
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.email
import com.example.androidproject1.service.core.ui.form.matches
import com.example.androidproject1.service.core.ui.form.minLength
import com.example.androidproject1.service.core.ui.form.required
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText

@Immutable
data class SignUpState(
    val email: FieldState = FieldState.of(required(), email()),
    val password: FieldState = FieldState.of(required(), minLength(MIN_PASSWORD_LENGTH)),
    val confirmPassword: FieldState = FieldState.of(required()),
    /** The terms and privacy policy, accepted. No account without it. */
    val termsAccepted: Boolean = false,
    /** Set once submit was pressed, so the box says why before anyone has touched it. */
    val termsTouched: Boolean = false,
) {

    /** What the box says: nothing until submit was pressed, then why it matters until ticked. */
    val termsError: UiText?
        get() = R.string.sign_up_terms_required.toUiText().takeIf { termsTouched && !termsAccepted }

    // The confirm rule needs the other field, so it is checked here rather than as a validator on
    // confirmPassword: a validator sees one value, and this one is about two.
    val passwordsMatch: Boolean get() = password.value == confirmPassword.value

    /**
     * What the confirm field shows: its own rule first, then the mismatch once it has been
     * touched. The `matches` validator is applied here rather than held on the field because a
     * validator on `confirmPassword` would capture the `password` it was constructed with, and
     * `copy` never reconstructs it.
     */
    val confirmPasswordError: UiText?
        get() = confirmPassword.error
            ?: matches(other = { password.value })(confirmPassword.value).takeIf { confirmPassword.touched }

    val canSubmit: Boolean get() =
        Form.canSubmit(email, password, confirmPassword) && passwordsMatch && termsAccepted

    /** The first thing wrong, in field order — what the snackbar repeats when submit is pressed anyway. */
    val firstError: UiText? get() = Form.firstError(email, password) ?: confirmPasswordError ?: termsError

    /** Anything typed. Back from here goes to Login and loses all three fields. */
    val isDirty: Boolean
        get() = email.value.isNotEmpty() ||
            password.value.isNotEmpty() ||
            confirmPassword.value.isNotEmpty()

    companion object {

        const val MIN_PASSWORD_LENGTH = 8

        val PREVIEW = SignUpState(
            email = FieldState.of(required(), email(), value = "ada@example.com"),
            password = FieldState.of(required(), minLength(MIN_PASSWORD_LENGTH), value = "hunter2!!"),
            confirmPassword = FieldState.of(required(), value = "hunter2!!"),
            termsAccepted = true,
        )
    }
}
