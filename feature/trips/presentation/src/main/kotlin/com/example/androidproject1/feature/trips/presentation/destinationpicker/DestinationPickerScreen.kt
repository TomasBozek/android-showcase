package com.example.androidproject1.feature.trips.presentation.destinationpicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppScrollShadow
import com.example.androidproject1.core.ui.component.list.ScrollEdge
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.feature.trips.presentation.R

@Composable
fun DestinationPickerScreen(
    state: DestinationPickerState,
    onEvent: (DestinationPickerEvent) -> Unit,
) {
    AppScaffold(
        screenId = "DestinationPickerScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.destination_picker_title),
                onNavigateUp = { onEvent(DestinationPickerEvent.NavigateUpClicked) },
                navigateUpTestTag = "destinationPicker_upButton",
            )
        },
        contentPadding = false,
    ) {
        val scroll = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = scroll,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("destinationPicker_list"),
            ) {
                items(items = state.destinations, key = { it.id }) { destination ->
                    AppListItem(
                        headline = destination.name,
                        supporting = destination.country,
                        onClick = { onEvent(DestinationPickerEvent.DestinationClicked(destination)) },
                        modifier = appAnimateItem().testTag("destinationPicker_item_${destination.id}"),
                    )
                }
            }
            // A long fixture list — the cue that there is more of it than the screen shows.
            AppScrollShadow(scroll, ScrollEdge.Top, Modifier.align(Alignment.TopCenter))
            AppScrollShadow(scroll, ScrollEdge.Bottom, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    DestinationPickerScreen(state = DestinationPickerState.PREVIEW) {}
}
