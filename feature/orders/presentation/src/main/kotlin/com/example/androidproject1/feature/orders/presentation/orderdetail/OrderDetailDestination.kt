package com.example.androidproject1.feature.orders.presentation.orderdetail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.layout.detailPane
import com.example.androidproject1.feature.orders.api.OrderDetailDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The screen of a route that carries arguments, [OrderDetailDestination]. `create_screen.py
 * --with-args` clones this set rather than the plain one; the difference is entirely in the key,
 * this file, the ViewModel and the test.
 */
fun EntryProviderScope<NavKey>.orderDetailDestination(navigator: Navigator) {
    // The detail half of its list's pair.
    entry<OrderDetailDestination>(metadata = detailPane()) { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: OrderDetailViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    OrderDetailNavigation.Back -> navigator.back()
                }
            },
        ) { state, onEvent ->
            OrderDetailScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
