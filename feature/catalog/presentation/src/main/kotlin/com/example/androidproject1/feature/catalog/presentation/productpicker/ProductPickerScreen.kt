package com.example.androidproject1.feature.catalog.presentation.productpicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.catalog.presentation.R
import com.example.androidproject1.service.locale.LocalFormats

@Composable
fun ProductPickerScreen(
    state: ProductPickerState,
    onEvent: (ProductPickerEvent) -> Unit,
) {
    AppScaffold(
        screenId = "ProductPickerScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.product_picker_title),
                onNavigateUp = { onEvent(ProductPickerEvent.NavigateUpClicked) },
                navigateUpTestTag = "productPicker_upButton",
            )
        },
    ) {
        val formats = LocalFormats.current
        if (state.products.isEmpty()) {
            // The picker reads the cache the catalog fills, so a cleared app has nothing to
            // offer until the catalog has been browsed once — which is what the action leads to.
            AppEmptyState(
                title = stringResource(R.string.product_picker_empty_title),
                message = stringResource(R.string.product_picker_empty_message),
                actionLabel = stringResource(R.string.product_picker_browse),
                onAction = { onEvent(ProductPickerEvent.BrowseCatalogClicked) },
                actionTestTag = "productPicker_browseButton",
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("productPicker_empty"),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("productPicker_list"),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
            ) {
                items(items = state.products, key = { it.id }) { product ->
                    AppListItem(
                        headline = product.name,
                        supporting = formats.money(product.price),
                        onClick = { onEvent(ProductPickerEvent.ProductClicked(product)) },
                        modifier = appAnimateItem().testTag("productPicker_item_${product.id}"),
                    )
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    ProductPickerScreen(state = ProductPickerState.PREVIEW) {}
}

/** A cleared app: nothing cached, and a way to the catalog that fills the cache. */
@ScreenPreview
@Composable
private fun EmptyPreview() = ThemedScreenPreview {
    ProductPickerScreen(state = ProductPickerState()) {}
}
