package com.example.androidproject1.feature.catalog.presentation.productdetail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.layout.detailPane
import com.example.androidproject1.feature.cart.api.CartDestination
import com.example.androidproject1.feature.catalog.api.ProductDetailDestination
import com.example.androidproject1.feature.home.api.HomeDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The added-to-cart snackbar's action selects the Cart tab, and the heart's selects Home,
 * where the favourites are shown — each by the other feature's api route.
 */
fun EntryProviderScope<NavKey>.productDetailDestination(
    navigator: Navigator,
) {
    // The detail half of the pair; see `productsDestination` for what the metadata buys.
    entry<ProductDetailDestination>(metadata = detailPane()) { key ->
        // The route key is passed straight into the ViewModel, so it is available in `init` and
        // comes back with the entry after process death.
        val viewModel: ProductDetailViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    ProductDetailNavigation.NavigateUp -> navigator.back()
                    ProductDetailNavigation.OpenCart -> navigator.selectTab(CartDestination)
                    ProductDetailNavigation.OpenHome -> navigator.selectTab(HomeDestination)
                }
            },
        ) { state, onEvent ->
            ProductDetailScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
