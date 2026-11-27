package com.example.androidproject1.feature.inventory.presentation.inventoryeditor

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.inventory.api.InventoryEditorDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.inventoryEditorDestination(navigator: Navigator) {
    entry<InventoryEditorDestination> { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: InventoryEditorViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    InventoryEditorNavigation.NavigateUp -> navigator.back()
                    // The list and the detail both observe the table, so there is nothing to hand back.
                    InventoryEditorNavigation.Saved -> navigator.back()
                }
            },
        ) { state, onEvent ->
            InventoryEditorScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
