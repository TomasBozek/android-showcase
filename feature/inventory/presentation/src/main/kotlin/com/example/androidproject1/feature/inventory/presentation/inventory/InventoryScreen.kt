package com.example.androidproject1.feature.inventory.presentation.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppFab
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.action.IconButtonKind
import com.example.androidproject1.core.ui.component.choice.AppCheckbox
import com.example.androidproject1.core.ui.component.choice.CheckState
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.content.AppReveal
import com.example.androidproject1.core.ui.component.field.AppSearchField
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppScrollShadow
import com.example.androidproject1.core.ui.component.list.ScrollEdge
import com.example.androidproject1.core.ui.component.list.SwipeAction
import com.example.androidproject1.core.ui.component.list.SwipeActions
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppToolbar
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.overlay.AppPickerSheet
import com.example.androidproject1.core.ui.component.status.AppAvatar
import com.example.androidproject1.core.ui.component.status.AppBadge
import com.example.androidproject1.core.ui.component.status.AppSkeleton
import com.example.androidproject1.core.ui.component.status.AppSpinner
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.core.ui.layout.appSharedElement
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.inventory.domain.ItemTag
import com.example.androidproject1.feature.inventory.domain.SyncState
import com.example.androidproject1.feature.inventory.presentation.R
import com.example.androidproject1.feature.inventory.presentation.component.ItemConditionTag
import com.example.androidproject1.feature.inventory.presentation.component.ItemFilterSheet
import com.example.androidproject1.feature.inventory.presentation.component.ItemSyncTag
import com.example.androidproject1.feature.inventory.presentation.component.itemOwnerKey
import com.example.androidproject1.service.locale.LocalFormats

@Composable
fun InventoryScreen(
    state: InventoryState,
    onEvent: (InventoryEvent) -> Unit,
) {
    AppScaffold(
        // Reached from Home, and the bottom bar is what leaves it, so there is no up arrow.
        screenId = "InventoryScreen",
        topBar = {
            // Selection mode covers the bar rather than replacing it: the toolbar slides
            // down over the top bar and back up, so the bar underneath never jumps. The Box is
            // as tall as the taller of the two, so nothing below moves either way.
            Box {
                AppTopBar(
                    title = stringResource(R.string.inventory_title),
                    actions = {
                        Box {
                            AppIconButton(
                                icon = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.inventory_filter_title),
                                onClick = { onEvent(InventoryEvent.FilterClicked) },
                                modifier = Modifier.testTag("inventory_filterButton"),
                            )
                            AppBadge(
                                count = state.filter.activeCount,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .testTag("inventory_filterBadge"),
                            )
                        }
                        // A picker sheet rather than a menu: three orders are a choice,
                        // every option is readable at once, and the current one is marked.
                        var sortOpen by remember { mutableStateOf(false) }
                        AppIconButton(
                            icon = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.inventory_sort_title),
                            onClick = { sortOpen = true },
                            modifier = Modifier.testTag("inventory_sortButton"),
                        )
                        if (sortOpen) {
                            val sorts = ItemSort.entries
                            AppPickerSheet(
                                title = stringResource(R.string.inventory_sort_title),
                                options = sorts.map { stringResource(it.labelRes()) },
                                selectedIndex = sorts.indexOf(state.sort),
                                onSelect = { onEvent(InventoryEvent.SortSelected(sorts[it])) },
                                onDismiss = { sortOpen = false },
                                optionTestTag = "inventory_sortItem",
                                modifier = Modifier.testTag("inventory_sortSheet"),
                            )
                        }
                    },
                )
                // What is selected, and what can be done to it — as tall as the bar it covers.
                AppReveal(visible = state.selecting, slide = true, modifier = Modifier.matchParentSize()) {
                    AppToolbar(
                        title = stringResource(R.string.inventory_selected_count, state.selectedIds.size),
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("inventory_selectionGroup"),
                    ) {
                        AppCheckbox(
                            checked = state.selectAllState,
                            onCheckedChange = { onEvent(InventoryEvent.SelectAllChanged(it)) },
                            label = stringResource(R.string.inventory_select_all),
                            inverse = true,
                            modifier = Modifier.testTag("inventory_selectAllCheckbox"),
                        )
                        AppIconButton(
                            icon = Icons.Filled.Favorite,
                            contentDescription = stringResource(R.string.inventory_favourite_selected),
                            onClick = { onEvent(InventoryEvent.FavouriteSelectedClicked) },
                            kind = IconButtonKind.Inverse,
                            modifier = Modifier.testTag("inventory_favouriteButton"),
                        )
                        AppIconButton(
                            icon = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.inventory_delete_selected),
                            onClick = { onEvent(InventoryEvent.DeleteSelectedClicked) },
                            kind = IconButtonKind.Inverse,
                            modifier = Modifier.testTag("inventory_deleteButton"),
                        )
                        AppIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.inventory_selection_clear),
                            onClick = { onEvent(InventoryEvent.SelectionCleared) },
                            kind = IconButtonKind.Inverse,
                            modifier = Modifier.testTag("inventory_selectionClearButton"),
                        )
                    }
                }
            }
        },
        contentPadding = false,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppSearchField(
                    value = state.query,
                    onValueChange = { onEvent(InventoryEvent.QueryChanged(it)) },
                    placeholder = stringResource(R.string.inventory_search_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.inset.lg, vertical = AppTheme.spacing.inset.md)
                        .testTag("inventory_searchField"),
                )
                when {
                    state.loading -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppTheme.spacing.inset.lg)
                            .testTag("inventory_skeleton"),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
                    ) {
                        repeat(SKELETON_ROWS) { AppSkeleton(height = AppTheme.density.listRowHeight) }
                    }

                    state.items.isEmpty() -> AppEmptyState(
                        title = stringResource(R.string.inventory_empty_title),
                        message = when {
                            state.query.isNotBlank() -> stringResource(
                                R.string.inventory_empty_filtered_message,
                                state.query,
                            )
                            !state.filter.isEmpty -> stringResource(R.string.inventory_empty_filter_message)
                            else -> stringResource(R.string.inventory_empty_message)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppTheme.spacing.inset.xl)
                            .testTag("inventory_empty"),
                    )

                    else -> {
                        val formats = LocalFormats.current
                        val scroll = rememberLazyListState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = scroll,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("inventory_list"),
                            ) {
                                items(items = state.items, key = { it.id }) { item ->
                                    val pending = item.id in state.pendingIds
                                    AppListItem(
                                        headline = item.name,
                                        supporting = "${item.owner} · ${formats.money(item.priceMinor)}",
                                        onClick = { onEvent(InventoryEvent.ItemClicked(item)) },
                                        onLongClick = { onEvent(InventoryEvent.ItemLongPressed(item)) },
                                        leading = {
                                            if (state.selecting) {
                                                AppCheckbox(
                                                    checked = if (item.id in
                                                        state.selectedIds
                                                    ) {
                                                        CheckState.On
                                                    } else {
                                                        CheckState.Off
                                                    },
                                                    onCheckedChange = { onEvent(InventoryEvent.ItemChecked(item, it)) },
                                                    // The row already says the name; the box says the state —
                                                    // and, to a screen reader, whose state.
                                                    label = "",
                                                    contentDescription = item.name,
                                                    modifier = Modifier.testTag("inventory_itemCheckbox"),
                                                )
                                            } else {
                                                // Travels to the detail's owner line: a
                                                // no-op outside the host's transition scope, so
                                                // no preview and no test sees a difference.
                                                AppAvatar(
                                                    name = item.owner,
                                                    modifier = Modifier.appSharedElement(itemOwnerKey(item.id)),
                                                )
                                            }
                                        },
                                        trailing = {
                                            when {
                                                pending -> AppSpinner(
                                                    modifier = Modifier.testTag("inventory_itemProgress"),
                                                )
                                                // A write the server has not taken yet says so before the condition does.
                                                item.sync != SyncState.Synced ->
                                                    ItemSyncTag(
                                                        sync = item.sync,
                                                        modifier = Modifier.testTag("inventory_syncBadge"),
                                                    )
                                                else -> ItemConditionTag(condition = item.condition)
                                            }
                                        },
                                        // A swipe reveals delete; the same delete is two
                                        // taps away in the detail's menu and in selection mode.
                                        swipeActions = SwipeActions(
                                            end = SwipeAction(
                                                label = stringResource(R.string.inventory_delete_selected),
                                                icon = Icons.Filled.Delete,
                                                tone = TagTone.Negative,
                                                testTag = "inventory_itemDeleteButton",
                                                onAction = { onEvent(InventoryEvent.ItemDeleteRequested(item)) },
                                            ),
                                        ),
                                        modifier = appAnimateItem().testTag("inventory_item_${item.id}"),
                                    )
                                }
                            }
                            // The cue that the list carries on past the fold — only while there is more.
                            AppScrollShadow(scroll, ScrollEdge.Top, Modifier.align(Alignment.TopCenter))
                            AppScrollShadow(scroll, ScrollEdge.Bottom, Modifier.align(Alignment.BottomCenter))
                        }
                    }
                }
            }

            // The key leaves while something is selected and comes back when nothing is.
            AppReveal(
                visible = !state.selecting,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppTheme.spacing.inset.lg),
            ) {
                AppFab(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.inventory_new_item),
                    onClick = { onEvent(InventoryEvent.NewItemClicked) },
                    modifier = Modifier.testTag("inventory_newButton"),
                )
            }
        }

        if (state.filterSheetOpen) {
            ItemFilterSheet(state = state, onEvent = onEvent)
        }
    }
}

private fun ItemSort.labelRes() = when (this) {
    ItemSort.Name -> R.string.inventory_sort_name
    ItemSort.Price -> R.string.inventory_sort_price
    ItemSort.Acquired -> R.string.inventory_sort_acquired
}

private const val SKELETON_ROWS = 6

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    InventoryScreen(state = InventoryState.PREVIEW) {}
}

@ScreenPreview
@Composable
private fun LoadingPreview() = ThemedScreenPreview {
    InventoryScreen(state = InventoryState.PREVIEW.copy(loading = true)) {}
}

@ScreenPreview
@Composable
private fun EmptyPreview() = ThemedScreenPreview {
    InventoryScreen(state = InventoryState.PREVIEW.copy(query = "piano", items = emptyList())) {}
}

@ScreenPreview
@Composable
private fun SelectingPreview() = ThemedScreenPreview {
    InventoryScreen(
        state = InventoryState.PREVIEW.copy(
            selectedIds = setOf("item-drill"),
            pendingIds = setOf("item-kettle"),
            filter = ItemFilter(tags = setOf(ItemTag.Lent)),
        ),
    ) {}
}
