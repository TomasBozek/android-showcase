package com.example.androidproject1.feature.settings.presentation.permissions

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.settings.api.SettingsPermissionsDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.settingsPermissionsDestination(navigator: Navigator) {
    entry<SettingsPermissionsDestination> {
        val viewModel: SettingsPermissionsViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    SettingsPermissionsNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            SettingsPermissionsScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
