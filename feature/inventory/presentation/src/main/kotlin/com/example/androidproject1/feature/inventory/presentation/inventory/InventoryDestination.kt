package com.example.androidproject1.feature.inventory.presentation.inventory

import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.layout.listPane
import com.example.androidproject1.feature.inventory.api.InventoryDestination
import com.example.androidproject1.feature.inventory.api.InventoryDetailDestination
import com.example.androidproject1.feature.inventory.api.InventoryEditorDestination
import com.example.androidproject1.feature.inventory.presentation.R
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

fun EntryProviderScope<NavKey>.inventoryDestination(navigator: Navigator) {
    // The list half of a list–detail pair: a wide window shows the detail beside it, and
    // the placeholder until something is picked; a phone is unchanged.
    entry<InventoryDestination>(
        metadata = listPane(
            detailPlaceholder = {
                AppEmptyState(
                    title = stringResource(R.string.inventory_detail_placeholder_title),
                    message = stringResource(R.string.inventory_detail_placeholder_message),
                )
            },
        ),
    ) {
        val viewModel: InventoryViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    is InventoryNavigation.OpenItem -> navigator.push(InventoryDetailDestination(navigation.itemId))
                    // A new item is a new id, minted here: the editor edits whatever id it is given
                    // and starts blank when nothing has that id.
                    InventoryNavigation.NewItem -> navigator.push(
                        InventoryEditorDestination(UUID.randomUUID().toString()),
                    )
                }
            },
        ) { state, onEvent ->
            InventoryScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
