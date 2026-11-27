package com.example.androidproject1.feature.catalog.presentation.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppPullToRefresh
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.status.AppBanner
import com.example.androidproject1.core.ui.component.status.BannerTone
import com.example.androidproject1.core.ui.layout.TabReselectEffect
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.catalog.presentation.R

@Composable
fun CategoriesScreen(
    state: CategoriesState,
    // Hoisted with a default, so the host's tab re-select and a test can reach it.
    listState: LazyListState = rememberLazyListState(),
    onEvent: (CategoriesEvent) -> Unit,
) {
    // A second tap on this tab pops to this root and scrolls it to the top.
    TabReselectEffect { listState.animateScrollToItem(0) }
    AppScaffold(
        screenId = "CategoriesScreen",
        // No up arrow: this is a tab root, and the bottom bar is what leaves it.
        topBar = {
            AppTopBar(title = stringResource(R.string.categories_title)) {
                AppIconButton(
                    icon = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.categories_search),
                    onClick = { onEvent(CategoriesEvent.SearchClicked) },
                    modifier = Modifier.testTag("categories_searchButton"),
                )
            }
        },
        contentPadding = false,
    ) {
        // Above the list, not over it: a stale catalog is a fact about every row below, and it
        // stays until a refresh gets through. A banner is one line, so the wording is the short
        // one — the snackbar's sentence is cut at a large font.
        AppBanner(
            text = stringResource(R.string.categories_stale),
            tone = BannerTone.Info,
            visible = state.stale,
            modifier = Modifier.testTag("categories_staleBadge"),
        )
        AppPullToRefresh(
            refreshing = state.refreshing,
            onRefresh = { onEvent(CategoriesEvent.RefreshRequested) },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.categories.isEmpty()) {
                // The scroll is what makes an empty screen pullable: the gesture needs something
                // that scrolls under it, and an empty state on its own does not.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    AppEmptyState(
                        title = stringResource(R.string.categories_empty_title),
                        message = stringResource(R.string.categories_empty_message),
                        actionLabel = stringResource(R.string.categories_retry),
                        onAction = { onEvent(CategoriesEvent.RefreshRequested) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("categories_empty"),
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("categories_list"),
                ) {
                    // Favourites first, a section of their own, drawn only when there are some.
                    if (state.favourites.isNotEmpty()) {
                        item(key = "favourites") {
                            AppSectionHeader(
                                title = stringResource(R.string.categories_favourites_title),
                                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
                            )
                        }
                        items(state.favourites, key = { "favourite:${it.id}" }) { product ->
                            AppListItem(
                                // The name only: a description is rich text, which the detail renders and a row would not.
                                headline = product.name,
                                modifier = appAnimateItem().testTag("categories_item_${product.id}"),
                                trailing = {
                                    AppIconButton(
                                        icon = Icons.Filled.Close,
                                        contentDescription = stringResource(
                                            R.string.categories_favourite_remove,
                                            product.name,
                                        ),
                                        onClick = { onEvent(CategoriesEvent.FavouriteRemoved(product.id)) },
                                        modifier = Modifier.testTag("categories_favouriteRemoveButton"),
                                    )
                                },
                            )
                        }
                        item(key = "all") {
                            AppSectionHeader(
                                title = stringResource(R.string.categories_all_title),
                                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
                            )
                        }
                    }
                    items(state.categories, key = { it.id }) { category ->
                        AppListItem(
                            headline = category.name,
                            onClick = { onEvent(CategoriesEvent.CategoryClicked(category)) },
                            modifier = appAnimateItem().testTag("categories_item_${category.id}"),
                        )
                        AppDivider(modifier = appAnimateItem())
                    }
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    CategoriesScreen(
        state = CategoriesState.PREVIEW,
    ) {}
}

/** Two favourites above the categories. */
@ScreenPreview
@Composable
private fun FavouritesPreview() = ThemedScreenPreview {
    CategoriesScreen(
        state = CategoriesState.PREVIEW_FAVOURITES,
    ) {}
}

/** Nothing came back: the message and its Retry, under the same top bar. */
@ScreenPreview
@Composable
private fun EmptyPreview() = ThemedScreenPreview {
    CategoriesScreen(
        state = CategoriesState.PREVIEW_EMPTY,
    ) {}
}

/** The cache over a dead network: the banner above the list. */
@ScreenPreview
@Composable
private fun StalePreview() = ThemedScreenPreview {
    CategoriesScreen(
        state = CategoriesState.PREVIEW_STALE,
    ) {}
}
