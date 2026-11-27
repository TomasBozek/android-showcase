package com.example.androidproject1.feature.onboarding.presentation.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.onboarding.api.OnboardingDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

/**
 * The root of the third flow, and the only screen in it.
 *
 * [navigator] is taken for the shape every destination has, and deliberately not used: there is
 * nowhere to go from the tour. Finishing writes the stored flag and `MainViewModel` replaces the
 * stack with the auth flow — a screen never switches flows itself.
 */
fun EntryProviderScope<NavKey>.onboardingDestination(
    @Suppress("unused") navigator: Navigator,
) {
    entry<OnboardingDestination> {
        val viewModel: OnboardingViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { },
        ) { state, onEvent ->
            OnboardingScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
