package com.example.androidproject1.feature.inventory.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.inventory.domain.SyncState
import com.example.androidproject1.feature.inventory.presentation.R

/**
 * The detail's word on an item's last write: its tag while it waits, and while it was
 * refused, what the user can do about it — try again, or discard the change. Draws nothing for a
 * synced item.
 */
@Composable
fun ItemSyncSection(
    sync: SyncState,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sync == SyncState.Synced) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
        ItemSyncTag(sync = sync, modifier = Modifier.testTag("inventoryDetail_syncBadge"))
        if (sync == SyncState.Failed) {
            AppText(text = stringResource(R.string.inventory_detail_sync_failed), role = TextRole.Body)
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md)) {
                AppButton(
                    label = stringResource(R.string.inventory_detail_sync_retry),
                    onClick = onRetry,
                    modifier = Modifier.testTag("inventoryDetail_retryButton"),
                )
                AppButton(
                    label = stringResource(R.string.inventory_detail_sync_discard),
                    onClick = onDiscard,
                    kind = ButtonKind.Neutral,
                    modifier = Modifier.testTag("inventoryDetail_discardButton"),
                )
            }
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    ItemSyncSection(sync = SyncState.Pending, onRetry = {}, onDiscard = {})
    ItemSyncSection(sync = SyncState.Failed, onRetry = {}, onDiscard = {})
}
