package com.example.androidproject1.feature.auth.presentation.resetpassword

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.auth.api.ResetPasswordDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.LocalNavResultStore
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * A route that carries arguments. `create_screen.py --with-args` clones this set rather than the
 * plain one; the difference is entirely in this file, the ViewModel and the test.
 */
/** What a finished reset hands back to Login, which shows the snackbar. */
const val PASSWORD_RESET_RESULT = "auth_password_reset"

fun EntryProviderScope<NavKey>.resetPasswordDestination(navigator: Navigator) {
    entry<ResetPasswordDestination> { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: ResetPasswordViewModel = koinViewModel { parametersOf(key) }
        val results = LocalNavResultStore.current

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    // Login says so when it comes back into view: a snackbar here would go with
                    // the screen that raised it.
                    ResetPasswordNavigation.Done -> {
                        results.set(PASSWORD_RESET_RESULT, true)
                        navigator.back()
                    }
                    ResetPasswordNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            ResetPasswordScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
