package com.example.androidproject1.feature.catalog.presentation.categories

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.catalog.api.CategoriesDestination
import com.example.androidproject1.feature.catalog.api.ProductSearchDestination
import com.example.androidproject1.feature.catalog.api.ProductsDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.categoriesDestination(navigator: Navigator) {
    entry<CategoriesDestination> {
        val viewModel: CategoriesViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    is CategoriesNavigation.Products -> navigator.push(
                        ProductsDestination(categoryId = navigation.categoryId, categoryName = navigation.categoryName),
                    )

                    CategoriesNavigation.Search -> navigator.push(ProductSearchDestination)
                }
            },
        ) { state, onEvent ->
            CategoriesScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
