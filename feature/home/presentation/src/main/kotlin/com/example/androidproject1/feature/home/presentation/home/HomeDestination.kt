package com.example.androidproject1.feature.home.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.home.api.HomeDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

/**
 * @param onOpen where a door leads — a tab to select or another feature's screen to push, which
 *   only the host knows, so it arrives as a lambda.
 * @param developerDoors whether this build has the developer screens; `false` in `prod`, where
 *   `DebugMenu.ENABLED` is a `const false` and the doors are never drawn.
 */
fun EntryProviderScope<NavKey>.homeDestination(
    navigator: Navigator,
    onOpen: (NavKey) -> Unit,
    developerDoors: Boolean,
    /** What `:app` shows above the doors — a server message. */
    headerContent: @Composable () -> Unit = {},
) {
    entry<HomeDestination> {
        val viewModel: HomeViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    is HomeNavigation.Open -> onOpen(navigation.route)
                    is HomeNavigation.Create -> navigator.push(navigation.route)
                }
            },
        ) { state, onEvent ->
            // Reported rather than passed to the screen: the screen renders from its state and
            // nothing else, so the build fact goes in through the same door a user event does.
            LaunchedEffect(developerDoors) {
                onEvent(HomeEvent.DeveloperDoorsAvailable(developerDoors))
            }
            HomeScreen(
                state = state,
                onEvent = onEvent,
                headerContent = headerContent,
            )
        }
    }
}
