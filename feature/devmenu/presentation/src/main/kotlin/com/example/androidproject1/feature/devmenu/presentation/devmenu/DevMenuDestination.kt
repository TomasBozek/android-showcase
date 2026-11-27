package com.example.androidproject1.feature.devmenu.presentation.devmenu

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.devmenu.api.DevMenuDestination
import com.example.androidproject1.feature.devmenu.api.DevMenuPlaygroundDestination
import com.example.androidproject1.feature.devmenu.api.NetworkLogDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.devMenuDestination(navigator: Navigator) {
    entry<DevMenuDestination> {
        val viewModel: DevMenuViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    DevMenuNavigation.NavigateUp -> navigator.back()
                    DevMenuNavigation.Playground -> navigator.push(DevMenuPlaygroundDestination())
                    DevMenuNavigation.NetworkLog -> navigator.push(NetworkLogDestination)
                    is DevMenuNavigation.Jump -> navigator.push(navigation.route)
                }
            },
        ) { state, onEvent ->
            DevMenuScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
