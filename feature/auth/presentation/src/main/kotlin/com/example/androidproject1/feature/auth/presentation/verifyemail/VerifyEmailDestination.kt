package com.example.androidproject1.feature.auth.presentation.verifyemail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.auth.api.VerifyEmailDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.verifyEmailDestination(navigator: Navigator) {
    entry<VerifyEmailDestination> { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: VerifyEmailViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    VerifyEmailNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            VerifyEmailScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
