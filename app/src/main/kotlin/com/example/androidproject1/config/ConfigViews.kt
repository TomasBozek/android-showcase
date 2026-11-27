package com.example.androidproject1.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.R
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.status.AppBanner
import com.example.androidproject1.core.ui.component.status.BannerTone
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * The soft update: a newer build is out and this one still works, so Home's header says so
 * once, with the store and a key that puts the offer away until a newer one comes.
 */
@Composable
fun UpdateBanner(onUpdate: () -> Unit, onDismiss: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
        modifier = Modifier.testTag("home_updateGroup"),
    ) {
        AppBanner(text = stringResource(R.string.main_update_available), tone = BannerTone.Info)
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm)) {
            AppButton(
                label = stringResource(R.string.main_update_action),
                onClick = onUpdate,
                kind = ButtonKind.Outline,
                size = ControlSize.Small,
                modifier = Modifier.testTag("home_updateButton"),
            )
            AppButton(
                label = stringResource(R.string.main_message_dismiss),
                onClick = onDismiss,
                kind = ButtonKind.Ghost,
                size = ControlSize.Small,
                modifier = Modifier.testTag("home_updateDismissButton"),
            )
        }
    }
}
