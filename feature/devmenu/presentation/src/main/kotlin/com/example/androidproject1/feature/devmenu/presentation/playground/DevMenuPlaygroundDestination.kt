package com.example.androidproject1.feature.devmenu.presentation.playground

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.devmenu.api.DevMenuPlaygroundDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.devMenuPlaygroundDestination(navigator: Navigator) {
    entry<DevMenuPlaygroundDestination> { key ->
        // The route key goes through Koin so the picked entry survives process death with the
        // back stack entry, the way GalleryDetailDestination does it.
        val viewModel: DevMenuPlaygroundViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    DevMenuPlaygroundNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            DevMenuPlaygroundScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
