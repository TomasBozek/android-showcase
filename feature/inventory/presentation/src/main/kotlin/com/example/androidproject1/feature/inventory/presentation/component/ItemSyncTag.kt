package com.example.androidproject1.feature.inventory.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.status.AppTag
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.feature.inventory.domain.SyncState
import com.example.androidproject1.feature.inventory.presentation.R

/**
 * Where an item's last write is, as a tag: waiting to be sent is information, refused is a
 * fault. Nothing for a synced item — the list and the detail decide where it goes, so the word and
 * the tone are decided once, here.
 */
@Composable
fun ItemSyncTag(
    sync: SyncState,
    modifier: Modifier = Modifier,
) {
    when (sync) {
        SyncState.Synced -> Unit
        SyncState.Pending -> AppTag(
            label = stringResource(R.string.inventory_sync_pending),
            tone = TagTone.Info,
            modifier = modifier,
        )
        SyncState.Failed -> AppTag(
            label = stringResource(R.string.inventory_sync_failed),
            tone = TagTone.Negative,
            modifier = modifier,
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    ItemSyncTag(sync = SyncState.Pending)
    ItemSyncTag(sync = SyncState.Failed)
}
