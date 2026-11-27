package com.example.androidproject1.feature.settings.presentation.language

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.settings.api.SettingsLanguageDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.settingsLanguageDestination(navigator: Navigator) {
    entry<SettingsLanguageDestination> {
        val viewModel: SettingsLanguageViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    SettingsLanguageNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            SettingsLanguageScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
