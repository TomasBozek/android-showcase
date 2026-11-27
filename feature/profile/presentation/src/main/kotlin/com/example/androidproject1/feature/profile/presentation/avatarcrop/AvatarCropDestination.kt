package com.example.androidproject1.feature.profile.presentation.avatarcrop

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.profile.api.AvatarCropDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The screen of a route that carries arguments, [AvatarCropDestination]. `create_screen.py
 * --with-args` clones this set rather than the plain one; the difference is entirely in the key,
 * this file, the ViewModel and the test.
 */
fun EntryProviderScope<NavKey>.avatarCropDestination(navigator: Navigator) {
    entry<AvatarCropDestination> { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: AvatarCropViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    AvatarCropNavigation.Back -> navigator.back()
                }
            },
        ) { state, onEvent ->
            AvatarCropScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
