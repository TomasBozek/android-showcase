package com.example.androidproject1.feature.cart.presentation.cart

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.cart.api.CartDestination
import com.example.androidproject1.feature.catalog.api.ProductPickerDestination
import com.example.androidproject1.feature.checkout.api.CheckoutAddressDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.NavResultEffect
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

/** The key the picker writes to. A constant, because only the cart asks for a product this way. */
const val CART_PICK_RESULT = "cart_picked_product"

/**
 * "Add a product" pushes the catalog's picker by its api route, [ProductPickerDestination], and
 * reads the id it hands back under [CART_PICK_RESULT].
 *
 * @param onProductPicked turns the id the picker returned into something the cart can hold. The
 * lookup is the catalog\'s business, so it happens outside this module too.
 */
fun EntryProviderScope<NavKey>.cartDestination(
    navigator: Navigator,
    onProductPicked: suspend (String) -> com.example.androidproject1.feature.cart.domain.CartItem?,
) {
    entry<CartDestination> {
        val viewModel: CartViewModel = koinViewModel()

        // The requester\'s half of core.4: fires once per result, and consumes it, so returning to
        // the cart later does not re-add a product the user already added.
        // NavResultEffect's callback is not suspending, and the lookup is — so it runs in the
        // ViewModel's scope, which is also where it belongs: an add that outlives this screen.
        NavResultEffect<String>(CART_PICK_RESULT) { productId ->
            viewModel.onProductPicked(productId, onProductPicked)
        }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    CartNavigation.PickProduct -> navigator.push(ProductPickerDestination(resultKey = CART_PICK_RESULT))
                    CartNavigation.Checkout -> navigator.push(CheckoutAddressDestination)
                }
            },
        ) { state, onEvent ->
            CartScreen(state = state, onEvent = onEvent)
        }
    }
}
