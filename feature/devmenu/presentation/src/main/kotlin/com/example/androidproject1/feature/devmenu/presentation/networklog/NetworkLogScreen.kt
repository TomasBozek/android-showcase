package com.example.androidproject1.feature.devmenu.presentation.networklog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.overlay.AppSheet
import com.example.androidproject1.feature.devmenu.presentation.R
import com.example.androidproject1.feature.devmenu.presentation.component.NetworkLogExchange
import com.example.androidproject1.feature.devmenu.presentation.component.summary

@Composable
fun NetworkLogScreen(
    state: NetworkLogState,
    onEvent: (NetworkLogEvent) -> Unit,
) {
    AppScaffold(
        screenId = "NetworkLogScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.network_log_title),
                onNavigateUp = { onEvent(NetworkLogEvent.UpClicked) },
                navigateUpTestTag = "networkLog_upButton",
                actions = {
                    AppIconButton(
                        icon = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.network_log_clear),
                        onClick = { onEvent(NetworkLogEvent.ClearClicked) },
                        enabled = state.entries.isNotEmpty(),
                        modifier = Modifier.testTag("networkLog_clearButton"),
                    )
                },
            )
        },
    ) {
        if (state.entries.isEmpty()) {
            AppEmptyState(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.network_log_empty_title),
                message = stringResource(R.string.network_log_empty_message),
                modifier = Modifier.testTag("networkLog_empty"),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().testTag("networkLog_list")) {
                items(items = state.entries, key = { it.id }) { entry ->
                    AppListItem(
                        headline = "${entry.method} ${entry.path}",
                        supporting = entry.summary(),
                        onClick = { onEvent(NetworkLogEvent.EntryClicked(entry.id)) },
                        modifier = Modifier.testTag("networkLog_item_${entry.id}"),
                    )
                }
            }
        }

        state.selected?.let { entry ->
            AppSheet(
                onDismiss = { onEvent(NetworkLogEvent.DetailDismissed) },
                title = stringResource(R.string.network_log_detail_title),
                modifier = Modifier.testTag("networkLog_detailSheet"),
            ) {
                NetworkLogExchange(entry = entry)
                AppButton(
                    label = stringResource(R.string.network_log_copy),
                    onClick = { onEvent(NetworkLogEvent.CopyClicked) },
                    kind = ButtonKind.Outline,
                    modifier = Modifier.fillMaxWidth().testTag("networkLog_copyButton"),
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    NetworkLogScreen(state = NetworkLogState.PREVIEW) {}
}

@ScreenPreview
@Composable
private fun EmptyPreview() = ThemedScreenPreview {
    NetworkLogScreen(state = NetworkLogState()) {}
}
