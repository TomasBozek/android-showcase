package com.example.androidproject1.feature.apphold.presentation.apphold

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.apphold.api.AppHoldDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The hold screen's entry. [navigator] is taken for the shape every destination has, and not
 * used: there is nowhere to go from a held app.
 */
fun EntryProviderScope<NavKey>.appHoldDestination(
    @Suppress("unused") navigator: Navigator,
) {
    entry<AppHoldDestination> { key ->
        val viewModel: AppHoldViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { },
        ) { state, onEvent ->
            AppHoldScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
