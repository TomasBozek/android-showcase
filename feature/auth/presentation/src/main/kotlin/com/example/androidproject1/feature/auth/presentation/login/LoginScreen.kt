package com.example.androidproject1.feature.auth.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.androidproject1.core.ui.common.TabletPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppLabelledDivider
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.navigation.DiscardBackHandler

@Composable
fun LoginScreen(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit,
) {
    AppScaffold(screenId = "LoginScreen") {
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
            AppText(text = stringResource(R.string.login_title), role = TextRole.Display)
            DiscardBackHandler(dirty = state.isDirty) { onEvent(LoginEvent.BackRequested) }

            AppTextField(
                value = state.email.value,
                errorText = state.email.error?.resolve(),
                onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
                label = stringResource(R.string.login_email),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                contentType = ContentType.Username + ContentType.EmailAddress,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_emailField"),
            )
            AppTextField(
                value = state.password.value,
                errorText = state.password.error?.resolve(),
                onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
                label = stringResource(R.string.login_password),
                password = true,
                imeAction = ImeAction.Done,
                onImeAction = { onEvent(LoginEvent.LoginClicked) },
                contentType = ContentType.Password,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_passwordField"),
            )
            // Enabled whether or not the form is valid: pressing it is what reveals the errors.
            // See LoginViewModel.submit.
            AppButton(
                label = stringResource(R.string.login_submit),
                onClick = { onEvent(LoginEvent.LoginClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_submitButton"),
            )
            AppButton(
                label = stringResource(R.string.login_forgot_password),
                onClick = { onEvent(LoginEvent.ForgotPasswordClicked) },
                kind = ButtonKind.Ghost,
                modifier = Modifier.testTag("login_forgotPasswordButton"),
            )
            AppLabelledDivider(label = stringResource(R.string.login_or))
            AppButton(
                label = stringResource(R.string.login_skip),
                onClick = { onEvent(LoginEvent.SkipLoginClicked) },
                kind = ButtonKind.Ghost,
                modifier = Modifier.testTag("login_skipButton"),
            )
            AppButton(
                label = stringResource(R.string.login_sign_up),
                onClick = { onEvent(LoginEvent.SignUpClicked) },
                kind = ButtonKind.Ghost,
                modifier = Modifier.testTag("login_signUpButton"),
            )
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    LoginScreen(
        state = LoginState.PREVIEW,
    ) {}
}

/** The error text under each field — the state a design review has to be able to see. */
@ScreenPreview
@Composable
private fun InvalidPreview() = ThemedScreenPreview {
    LoginScreen(
        state = LoginState.PREVIEW_INVALID,
    ) {}
}

/** Sign-in at a tablet's width: two fields and a key, centred rather than spread. */
@TabletPreview
@Composable
private fun TabletPreview() = ThemedScreenPreview {
    LoginScreen(state = LoginState.PREVIEW) {}
}
