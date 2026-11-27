package com.example.androidproject1.feature.auth.presentation.login

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.auth.api.ForgotPasswordDestination
import com.example.androidproject1.feature.auth.api.LoginDestination
import com.example.androidproject1.feature.auth.api.SignUpDestination
import com.example.androidproject1.feature.auth.presentation.resetpassword.PASSWORD_RESET_RESULT
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.NavResultEffect
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.loginDestination(navigator: Navigator) {
    entry<LoginDestination> {
        val viewModel: LoginViewModel = koinViewModel()
        // A reset link finished on its own screen and came back here.
        NavResultEffect<Boolean>(PASSWORD_RESET_RESULT) { viewModel.onPasswordReset() }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    LoginNavigation.SignUp -> navigator.push(SignUpDestination)
                    LoginNavigation.ForgotPassword -> navigator.push(ForgotPasswordDestination)
                }
            },
        ) { state, onEvent ->
            LoginScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
