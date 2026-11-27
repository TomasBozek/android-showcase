package com.example.androidproject1.feature.devmenu.presentation.networklog

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.devmenu.api.NetworkLogDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.networkLogDestination(navigator: Navigator) {
    entry<NetworkLogDestination> {
        val viewModel: NetworkLogViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    NetworkLogNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            NetworkLogScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
