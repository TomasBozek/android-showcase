package com.example.androidproject1.feature.apphold.presentation.apphold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.apphold.api.AppHoldReason
import com.example.androidproject1.feature.apphold.presentation.R

/**
 * The whole app while the server says it may not run: why, and — for an update — the one
 * way on. Nothing else is drawn, so there is nothing to tap past it.
 */
@Composable
fun AppHoldScreen(
    state: AppHoldState,
    onEvent: (AppHoldEvent) -> Unit,
) {
    val update = state.reason == AppHoldReason.UpdateRequired
    AppScaffold(screenId = "AppHoldScreen") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.spacing.inset.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppText(
                text = stringResource(
                    if (update) R.string.app_hold_update_title else R.string.app_hold_maintenance_title,
                ),
                role = TextRole.Title,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("appHold_titleValue"),
            )
            AppText(
                text = stringResource(
                    if (update) R.string.app_hold_update_body else R.string.app_hold_maintenance_body,
                ),
                role = TextRole.Secondary,
                textAlign = TextAlign.Center,
            )
            if (update) {
                AppButton(
                    label = stringResource(R.string.app_hold_update_action),
                    onClick = { onEvent(AppHoldEvent.UpdateClicked) },
                    modifier = Modifier.testTag("appHold_updateButton"),
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview(
    @PreviewParameter(AppHoldStatePreviews::class) state: AppHoldState,
) = ThemedScreenPreview {
    AppHoldScreen(state = state) {}
}
