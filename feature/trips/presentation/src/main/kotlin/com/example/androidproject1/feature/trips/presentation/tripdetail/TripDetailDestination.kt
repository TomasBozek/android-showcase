package com.example.androidproject1.feature.trips.presentation.tripdetail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.layout.detailPane
import com.example.androidproject1.feature.trips.api.TripDetailDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.tripDetailDestination(navigator: Navigator) {
    // The detail half of its list's pair.
    entry<TripDetailDestination>(metadata = detailPane()) { key ->
        val viewModel: TripDetailViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    TripDetailNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            TripDetailScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
