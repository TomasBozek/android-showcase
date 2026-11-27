package com.example.androidproject1.feature.checkout.presentation.checkoutreview

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.checkout.api.CheckoutReviewDestination
import com.example.androidproject1.feature.orders.api.OrderDetailDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.checkoutReviewDestination(navigator: Navigator) {
    entry<CheckoutReviewDestination> {
        val viewModel: CheckoutReviewViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    // The wizard gives way to the order it placed: review, payment and delivery pop, and the
                    // address step is replaced, so back from the order lands on the cart.
                    is CheckoutReviewNavigation.Placed -> {
                        repeat(STEPS_ABOVE_ADDRESS) { navigator.back() }
                        navigator.replace(OrderDetailDestination(orderId = navigation.orderId))
                    }
                    CheckoutReviewNavigation.Payment -> navigator.back()
                    CheckoutReviewNavigation.Back -> navigator.back()
                }
            },
        ) { state, onEvent ->
            CheckoutReviewScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

/** Review, payment and delivery, above the address step the wizard started with. */
private const val STEPS_ABOVE_ADDRESS = 3
