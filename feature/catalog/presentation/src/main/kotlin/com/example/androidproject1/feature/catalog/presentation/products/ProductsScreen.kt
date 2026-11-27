package com.example.androidproject1.feature.catalog.presentation.products

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppPullToRefresh
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.core.ui.layout.appSharedElement
import com.example.androidproject1.feature.catalog.presentation.component.sharedNameKey
import com.example.androidproject1.feature.catalog.presentation.component.sharedPriceKey
import com.example.androidproject1.service.locale.LocalFormats

@Composable
fun ProductsScreen(
    state: ProductsState,
    onEvent: (ProductsEvent) -> Unit,
) {
    AppScaffold(
        screenId = "ProductsScreen",
        topBar = {
            AppTopBar(
                title = state.categoryName,
                onNavigateUp = { onEvent(ProductsEvent.NavigateUpClicked) },
                navigateUpTestTag = "products_upButton",
            )
        },
        contentPadding = false,
    ) {
        val formats = LocalFormats.current
        AppPullToRefresh(
            refreshing = state.refreshing,
            onRefresh = { onEvent(ProductsEvent.RefreshRequested) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("products_list"),
            ) {
                items(state.products, key = { it.id }) { product ->
                    // The name and the price travel to the detail: the same keys are on
                    // the detail's title and figure, and the modifier is a no-op wherever no
                    // transition is running — in this preview, in a test, on a wide window.
                    AppListItem(
                        headline = product.name,
                        onClick = { onEvent(ProductsEvent.ProductClicked(product)) },
                        modifier = appAnimateItem().testTag("products_item_${product.id}"),
                        headlineModifier = Modifier.appSharedElement(product.sharedNameKey()),
                        // A price is numeric, so it gets tabular figures and lines up down the
                        // column.
                        trailing = {
                            AppText(
                                text = formats.money(product.price),
                                role = TextRole.Numeric,
                                modifier = Modifier.appSharedElement(product.sharedPriceKey()),
                            )
                        },
                    )
                    AppDivider(modifier = appAnimateItem())
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    ProductsScreen(
        state = ProductsState.PREVIEW,
    ) {}
}
