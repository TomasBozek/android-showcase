package com.example.androidproject1.feature.settings.presentation.settingsstorage

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.settings.api.SettingsStorageDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.settingsStorageDestination(navigator: Navigator) {
    entry<SettingsStorageDestination> {
        val viewModel: SettingsStorageViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    SettingsStorageNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            SettingsStorageScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
