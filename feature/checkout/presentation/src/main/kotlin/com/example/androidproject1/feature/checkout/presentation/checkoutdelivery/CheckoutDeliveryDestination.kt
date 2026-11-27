package com.example.androidproject1.feature.checkout.presentation.checkoutdelivery

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.checkout.api.CheckoutDeliveryDestination
import com.example.androidproject1.feature.checkout.api.CheckoutPaymentDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.checkoutDeliveryDestination(navigator: Navigator) {
    entry<CheckoutDeliveryDestination> {
        val viewModel: CheckoutDeliveryViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    CheckoutDeliveryNavigation.Payment -> navigator.push(CheckoutPaymentDestination)
                    CheckoutDeliveryNavigation.Back -> navigator.back()
                }
            },
        ) { state, onEvent ->
            CheckoutDeliveryScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
