package com.example.androidproject1.feature.settings.presentation.about

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.settings.api.AboutDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

/**
 * @param openLicences shows the open-source licences. Their screen is Play services', which only
 *   `:app` depends on, so it arrives as a lambda wired in `AppNavHost`.
 */
fun EntryProviderScope<NavKey>.aboutDestination(
    navigator: Navigator,
    openLicences: () -> Unit,
) {
    entry<AboutDestination> {
        val viewModel: AboutViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    AboutNavigation.NavigateUp -> navigator.back()
                    AboutNavigation.Licences -> openLicences()
                }
            },
        ) { state, onEvent ->
            AboutScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
