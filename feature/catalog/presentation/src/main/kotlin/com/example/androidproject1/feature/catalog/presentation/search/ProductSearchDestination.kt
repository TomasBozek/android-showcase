package com.example.androidproject1.feature.catalog.presentation.search

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.catalog.api.ProductDetailDestination
import com.example.androidproject1.feature.catalog.api.ProductSearchDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.productSearchDestination(navigator: Navigator) {
    entry<ProductSearchDestination> {
        val viewModel: ProductSearchViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    // The same feature, so this is an ordinary push rather than a lambda: only a
                    // jump into another feature's presentation has to be wired in AppNavHost.
                    is ProductSearchNavigation.ProductDetail ->
                        navigator.push(ProductDetailDestination(productId = navigation.productId))

                    ProductSearchNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            ProductSearchScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
