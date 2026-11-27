package com.example.androidproject1.feature.profile.presentation.profile

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.profile.api.ProfileDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.profileDestination(navigator: Navigator) {
    entry<ProfileDestination> {
        val viewModel: ProfileViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    ProfileNavigation.NavigateUp -> navigator.back()
                    is ProfileNavigation.Open -> navigator.push(navigation.route)
                }
            },
        ) { state, onEvent ->
            ProfileScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
