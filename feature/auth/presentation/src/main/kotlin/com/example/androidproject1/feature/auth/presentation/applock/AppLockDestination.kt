package com.example.androidproject1.feature.auth.presentation.applock

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.auth.api.AppLockDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.appLockDestination(navigator: Navigator) {
    entry<AppLockDestination> {
        val viewModel: AppLockViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            // Turn each AppLockNavigation case into a navigator call here — `navigator.push(Key)`
            // to push, `navigator.back()` to pop. Another feature is reached the same way, by the
            // route key in its api module.
            onNavigation = { navigation ->
            },
        ) { state, onEvent ->
            AppLockScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
