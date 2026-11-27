package com.example.androidproject1.feature.auth.presentation.forgotpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.navigation.DiscardBackHandler

/**
 * One address in, one link out. After the request the form gives way to "check your inbox", with
 * a resend that waits thirty seconds — the same address, so there is nothing to retype.
 */
@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onEvent: (ForgotPasswordEvent) -> Unit,
) {
    AppScaffold(
        screenId = "ForgotPasswordScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.forgot_password_title),
                onNavigateUp = { onEvent(ForgotPasswordEvent.NavigateUpClicked) },
                navigateUpTestTag = "forgotPassword_upButton",
            )
        },
    ) {
        DiscardBackHandler(dirty = state.isDirty) { onEvent(ForgotPasswordEvent.BackRequested) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            if (!state.sent) {
                AppText(text = stringResource(R.string.forgot_password_intro), role = TextRole.Secondary)
                AppTextField(
                    value = state.email.value,
                    errorText = state.email.error?.resolve(),
                    onValueChange = { onEvent(ForgotPasswordEvent.EmailChanged(it)) },
                    label = stringResource(R.string.forgot_password_email),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onImeAction = { onEvent(ForgotPasswordEvent.SubmitClicked) },
                    contentType = ContentType.Username + ContentType.EmailAddress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgotPassword_emailField"),
                )
                // Enabled on an invalid form on purpose: pressing it is what says why.
                AppButton(
                    label = stringResource(R.string.forgot_password_submit),
                    onClick = { onEvent(ForgotPasswordEvent.SubmitClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgotPassword_submitButton"),
                )
            } else {
                AppText(
                    text = stringResource(R.string.forgot_password_sent, state.email.value),
                    role = TextRole.BodyLarge,
                    modifier = Modifier.testTag("forgotPassword_sentValue"),
                )
                AppButton(
                    label = if (state.resendInSeconds > 0) {
                        pluralStringResource(
                            R.plurals.forgot_password_resend_in,
                            state.resendInSeconds,
                            state.resendInSeconds,
                        )
                    } else {
                        stringResource(R.string.forgot_password_resend)
                    },
                    onClick = { onEvent(ForgotPasswordEvent.ResendClicked) },
                    enabled = state.resendInSeconds == 0,
                    kind = ButtonKind.Outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgotPassword_resendButton"),
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    ForgotPasswordScreen(state = ForgotPasswordState.PREVIEW) {}
}

/** After the request: the inbox message and a resend still counting down. */
@ScreenPreview
@Composable
private fun SentPreview() = ThemedScreenPreview {
    ForgotPasswordScreen(state = ForgotPasswordState.PREVIEW_SENT) {}
}
