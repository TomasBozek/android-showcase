package com.example.androidproject1.feature.auth.presentation.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.choice.AppCheckbox
import com.example.androidproject1.core.ui.component.choice.CheckState
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.navigation.DiscardBackHandler

@Composable
fun SignUpScreen(
    state: SignUpState,
    onEvent: (SignUpEvent) -> Unit,
) {
    AppScaffold(screenId = "SignUpScreen") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // safeDrawingPadding in AppScaffold shrinks the content area when the keyboard
                // opens, and a large font makes this form taller than a short phone even without
                // one — a form that cannot reach its own submit button is a form nobody can use.
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(
                AppTheme.spacing.stack.md,
                Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppText(text = stringResource(R.string.sign_up_title), role = TextRole.Display)
            DiscardBackHandler(dirty = state.isDirty) { onEvent(SignUpEvent.BackRequested) }

            AppTextField(
                value = state.email.value,
                errorText = state.email.error?.resolve(),
                onValueChange = { onEvent(SignUpEvent.EmailChanged(it)) },
                label = stringResource(R.string.sign_up_email),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                contentType = ContentType.Username + ContentType.EmailAddress,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signUp_emailField"),
            )
            AppTextField(
                value = state.password.value,
                errorText = state.password.error?.resolve(),
                onValueChange = { onEvent(SignUpEvent.PasswordChanged(it)) },
                label = stringResource(R.string.sign_up_password),
                password = true,
                imeAction = ImeAction.Next,
                // NewPassword, not Password: this is the field a manager should offer to *generate*
                // into, and the one below is what it repeats.
                contentType = ContentType.NewPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signUp_passwordField"),
            )
            AppTextField(
                value = state.confirmPassword.value,
                errorText = state.confirmPasswordError?.resolve(),
                onValueChange = { onEvent(SignUpEvent.ConfirmPasswordChanged(it)) },
                label = stringResource(R.string.sign_up_confirm_password),
                password = true,
                imeAction = ImeAction.Done,
                onImeAction = { onEvent(SignUpEvent.SignUpClicked) },
                contentType = ContentType.NewPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signUp_confirmPasswordField"),
            )
            // The consent box sits above the key, and the two documents it names are keys of their
            // own under it rather than links inside its label: an inline link is a word-sized
            // target with no name of its own, which is what `assertAccessible` refuses.
            AppCheckbox(
                checked = if (state.termsAccepted) CheckState.On else CheckState.Off,
                onCheckedChange = { onEvent(SignUpEvent.TermsToggled(it)) },
                label = stringResource(R.string.sign_up_terms),
                errorText = state.termsError?.resolve(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signUp_termsCheckbox"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm)) {
                AppButton(
                    label = stringResource(R.string.sign_up_terms_link),
                    onClick = { onEvent(SignUpEvent.TermsLinkClicked) },
                    kind = ButtonKind.Ghost,
                    size = ControlSize.Small,
                    modifier = Modifier.testTag("signUp_termsButton"),
                )
                AppButton(
                    label = stringResource(R.string.sign_up_privacy_link),
                    onClick = { onEvent(SignUpEvent.PrivacyLinkClicked) },
                    kind = ButtonKind.Ghost,
                    size = ControlSize.Small,
                    modifier = Modifier.testTag("signUp_privacyButton"),
                )
            }
            // Enabled whether or not the form is valid: pressing it is what reveals the errors,
            // the unticked box among them. See SignUpViewModel.submit.
            AppButton(
                label = stringResource(R.string.sign_up_submit),
                onClick = { onEvent(SignUpEvent.SignUpClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signUp_submitButton"),
            )
            AppButton(
                label = stringResource(R.string.sign_up_login),
                onClick = { onEvent(SignUpEvent.LoginClicked) },
                kind = ButtonKind.Ghost,
                modifier = Modifier.testTag("signUp_loginButton"),
            )
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    SignUpScreen(
        state = SignUpState.PREVIEW,
    ) {}
}
