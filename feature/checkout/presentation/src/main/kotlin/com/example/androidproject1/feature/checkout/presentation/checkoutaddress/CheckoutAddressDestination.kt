package com.example.androidproject1.feature.checkout.presentation.checkoutaddress

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.checkout.api.CheckoutAddressDestination
import com.example.androidproject1.feature.checkout.api.CheckoutDeliveryDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

/** The wizard's entry: the cart pushes [CheckoutAddressDestination] from another feature. */
fun EntryProviderScope<NavKey>.checkoutAddressDestination(navigator: Navigator) {
    entry<CheckoutAddressDestination> {
        val viewModel: CheckoutAddressViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    CheckoutAddressNavigation.Delivery -> navigator.push(CheckoutDeliveryDestination)
                    CheckoutAddressNavigation.Back -> navigator.back()
                }
            },
        ) { state, onEvent ->
            CheckoutAddressScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
