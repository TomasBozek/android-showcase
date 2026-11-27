package com.example.androidproject1.feature.auth.presentation.forgotpassword

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.auth.api.ForgotPasswordDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.forgotPasswordDestination(navigator: Navigator) {
    entry<ForgotPasswordDestination> {
        val viewModel: ForgotPasswordViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    ForgotPasswordNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            ForgotPasswordScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
