package com.example.androidproject1.feature.inventory.presentation.inventorydetail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.layout.detailPane
import com.example.androidproject1.feature.inventory.api.InventoryDetailDestination
import com.example.androidproject1.feature.inventory.api.InventoryEditorDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.inventoryDetailDestination(navigator: Navigator) {
    // The detail half of its list's pair.
    entry<InventoryDetailDestination>(metadata = detailPane()) { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: InventoryDetailViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    is InventoryDetailNavigation.Edit -> navigator.push(InventoryEditorDestination(navigation.itemId))
                    InventoryDetailNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            InventoryDetailScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
