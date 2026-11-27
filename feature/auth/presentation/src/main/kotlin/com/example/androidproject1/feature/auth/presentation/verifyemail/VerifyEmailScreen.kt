package com.example.androidproject1.feature.auth.presentation.verifyemail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppCodeField
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.service.core.ui.text.resolve

/** The six digits sign-up mailed, one key, and another code once thirty seconds have passed. */
@Composable
fun VerifyEmailScreen(
    state: VerifyEmailState,
    onEvent: (VerifyEmailEvent) -> Unit,
) {
    AppScaffold(
        screenId = "VerifyEmailScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.verify_email_title),
                onNavigateUp = { onEvent(VerifyEmailEvent.NavigateUpClicked) },
                navigateUpTestTag = "verifyEmail_upButton",
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            AppText(text = stringResource(R.string.verify_email_intro, state.email), role = TextRole.BodyLarge)
            AppCodeField(
                value = state.code,
                onValueChange = { onEvent(VerifyEmailEvent.CodeChanged(it)) },
                label = stringResource(R.string.verify_email_code),
                errorText = state.codeError?.resolve(),
                onDone = { onEvent(VerifyEmailEvent.SubmitClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verifyEmail_codeField"),
            )
            // Enabled on an incomplete code on purpose: pressing it is what says why.
            AppButton(
                label = stringResource(R.string.verify_email_submit),
                onClick = { onEvent(VerifyEmailEvent.SubmitClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verifyEmail_submitButton"),
            )
            AppButton(
                label = if (state.resendInSeconds > 0) {
                    pluralStringResource(R.plurals.verify_email_resend_in, state.resendInSeconds, state.resendInSeconds)
                } else {
                    stringResource(R.string.verify_email_resend)
                },
                onClick = { onEvent(VerifyEmailEvent.ResendClicked) },
                enabled = state.resendInSeconds == 0,
                kind = ButtonKind.Ghost,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verifyEmail_resendButton"),
            )
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    VerifyEmailScreen(state = VerifyEmailState.PREVIEW) {}
}
