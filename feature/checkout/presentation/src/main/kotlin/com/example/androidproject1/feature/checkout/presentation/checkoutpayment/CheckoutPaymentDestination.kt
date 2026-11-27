package com.example.androidproject1.feature.checkout.presentation.checkoutpayment

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.checkout.api.CheckoutPaymentDestination
import com.example.androidproject1.feature.checkout.api.CheckoutReviewDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.checkoutPaymentDestination(navigator: Navigator) {
    entry<CheckoutPaymentDestination> {
        val viewModel: CheckoutPaymentViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    CheckoutPaymentNavigation.Review -> navigator.push(CheckoutReviewDestination)
                    CheckoutPaymentNavigation.Back -> navigator.back()
                }
            },
        ) { state, onEvent ->
            CheckoutPaymentScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
