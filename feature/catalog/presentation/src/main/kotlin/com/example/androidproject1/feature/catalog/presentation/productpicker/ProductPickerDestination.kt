package com.example.androidproject1.feature.catalog.presentation.productpicker

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.catalog.api.CategoriesDestination
import com.example.androidproject1.feature.catalog.api.ProductPickerDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import com.example.androidproject1.service.navigation.rememberNavResultSender
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.productPickerDestination(
    navigator: Navigator,
) {
    entry<ProductPickerDestination> { key ->
        val viewModel: ProductPickerViewModel = koinViewModel { parametersOf(key) }
        val setNavResult = rememberNavResultSender(key.resultKey)

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    is ProductPickerNavigation.Picked -> {
                        // Set, then pop: the store lives above the entries, so the value outlives
                        // this screen leaving the back stack.
                        setNavResult(navigation.productId)
                        navigator.back()
                    }

                    // No result set, so the requester sees nothing arrive.
                    ProductPickerNavigation.NavigateUp -> navigator.back()

                    // Pop first: a picker left under the catalog tab would still be waiting on
                    // the cart's stack when the user came back to it. Then "Browse the catalog"
                    // selects the catalog tab, whose root is this feature's own.
                    ProductPickerNavigation.OpenCatalog -> {
                        navigator.back()
                        navigator.selectTab(CategoriesDestination)
                    }
                }
            },
        ) { state, onEvent ->
            ProductPickerScreen(state = state, onEvent = onEvent)
        }
    }
}
