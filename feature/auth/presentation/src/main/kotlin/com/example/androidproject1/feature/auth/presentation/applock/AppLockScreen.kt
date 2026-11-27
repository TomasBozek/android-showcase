package com.example.androidproject1.feature.auth.presentation.applock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.auth.presentation.R
import com.example.androidproject1.feature.auth.presentation.component.AppLockHeadline

/**
 * What a held app shows: drawn by `MainActivity` over the main flow, which stays where it
 * was underneath. The system's prompt is asked for as it appears; the button asks again.
 */
@Composable
fun AppLockScreen(
    state: AppLockState,
    onEvent: (AppLockEvent) -> Unit,
) {
    AppScaffold(screenId = "AppLockScreen") {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(
                AppTheme.spacing.stack.md,
                Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLockHeadline(
                text = stringResource(R.string.app_lock_title),
                modifier = Modifier.testTag("appLock_titleValue"),
            )
            AppText(
                text = stringResource(R.string.app_lock_message),
                role = TextRole.Body,
            )
            AppButton(
                label = stringResource(R.string.app_lock_unlock),
                onClick = { onEvent(AppLockEvent.UnlockClicked) },
                loading = state.asking,
                modifier = Modifier.testTag("appLock_unlockButton"),
            )
        }
    }
}

@ScreenPreview
@Composable
private fun Preview(
    @PreviewParameter(AppLockStatePreviews::class) state: AppLockState,
) = ThemedScreenPreview {
    AppLockScreen(
        state = state,
    ) {}
}
