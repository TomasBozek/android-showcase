package com.example.androidproject1.feature.trips.presentation.destinationpicker

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.trips.api.DestinationPickerDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import com.example.androidproject1.service.navigation.rememberNavResultSender
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.destinationPickerDestination(navigator: Navigator) {
    entry<DestinationPickerDestination> { key ->
        val viewModel: DestinationPickerViewModel = koinViewModel { parametersOf(key) }
        val setNavResult = rememberNavResultSender(key.resultKey)

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    is DestinationPickerNavigation.Picked -> {
                        // Set, then pop: the store lives above the entries, so the value outlives
                        // this screen leaving the back stack.
                        setNavResult(navigation.destinationId)
                        navigator.back()
                    }

                    // No result set, so the requester sees nothing arrive.
                    DestinationPickerNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            DestinationPickerScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
