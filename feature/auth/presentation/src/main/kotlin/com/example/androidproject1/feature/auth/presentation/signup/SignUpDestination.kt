package com.example.androidproject1.feature.auth.presentation.signup

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.auth.api.SignUpDestination
import com.example.androidproject1.feature.auth.api.VerifyEmailDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.signUpDestination(navigator: Navigator) {
    entry<SignUpDestination> {
        val viewModel: SignUpViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    SignUpNavigation.Login -> navigator.back()
                    is SignUpNavigation.VerifyEmail -> navigator.push(VerifyEmailDestination(email = navigation.email))
                }
            },
        ) { state, onEvent ->
            SignUpScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
