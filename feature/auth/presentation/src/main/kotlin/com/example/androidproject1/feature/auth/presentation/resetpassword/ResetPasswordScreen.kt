package com.example.androidproject1.feature.auth.presentation.resetpassword

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.navigation.DiscardBackHandler

/** Where a reset link lands: the new password twice, and one key. */
@Composable
fun ResetPasswordScreen(
    state: ResetPasswordState,
    onEvent: (ResetPasswordEvent) -> Unit,
) {
    AppScaffold(
        screenId = "ResetPasswordScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.reset_password_title),
                onNavigateUp = { onEvent(ResetPasswordEvent.NavigateUpClicked) },
                navigateUpTestTag = "resetPassword_upButton",
            )
        },
    ) {
        DiscardBackHandler(dirty = state.isDirty) { onEvent(ResetPasswordEvent.BackRequested) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            AppText(text = stringResource(R.string.reset_password_intro), role = TextRole.Secondary)
            AppTextField(
                value = state.password.value,
                errorText = state.password.error?.resolve(),
                onValueChange = { onEvent(ResetPasswordEvent.PasswordChanged(it)) },
                label = stringResource(R.string.reset_password_password),
                password = true,
                imeAction = ImeAction.Next,
                // NewPassword: the field a manager should offer to generate, and then save.
                contentType = ContentType.NewPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resetPassword_passwordField"),
            )
            AppTextField(
                value = state.confirmPassword.value,
                errorText = state.confirmPasswordError?.resolve(),
                onValueChange = { onEvent(ResetPasswordEvent.ConfirmPasswordChanged(it)) },
                label = stringResource(R.string.reset_password_confirm),
                password = true,
                imeAction = ImeAction.Done,
                onImeAction = { onEvent(ResetPasswordEvent.SubmitClicked) },
                contentType = ContentType.NewPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resetPassword_confirmPasswordField"),
            )
            // Enabled on a bad pair on purpose: pressing it is what says why.
            AppButton(
                label = stringResource(R.string.reset_password_submit),
                onClick = { onEvent(ResetPasswordEvent.SubmitClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resetPassword_submitButton"),
            )
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    ResetPasswordScreen(state = ResetPasswordState.PREVIEW) {}
}

/** The two passwords differ: the confirm field says so. */
@ScreenPreview
@Composable
private fun MismatchPreview() = ThemedScreenPreview {
    ResetPasswordScreen(state = ResetPasswordState.PREVIEW_MISMATCH) {}
}
