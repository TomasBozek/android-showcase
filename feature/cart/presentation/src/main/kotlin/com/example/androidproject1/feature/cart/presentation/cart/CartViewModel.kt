package com.example.androidproject1.feature.cart.presentation.cart

import com.example.androidproject1.feature.cart.domain.CartItem
import com.example.androidproject1.feature.cart.domain.CartRepository
import com.example.androidproject1.feature.cart.presentation.R
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.update

class CartViewModel(
    logger: Logger,
    private val cartRepository: CartRepository,
) : BaseViewModel<CartState, CartEvent, CartNavigation>(
    // The empty cart is a real state worth rendering, not an absence — so no overlay on first frame.
    initialState = CartState(),
    logger = logger.withTag("CartViewModel"),
) {

    companion object {

        const val SNACKBAR_UNDO_REMOVE = "cart_undo_remove"
    }

    /** What the last remove took away, so the snackbar action has something to put back. */
    private var lastRemoved: CartItem? = null

    init {
        observeItems()
    }

    override fun onUiEvent(event: CartEvent) {
        when (event) {
            is CartEvent.QuantityChanged ->
                write { cartRepository.setQuantity(event.productId, event.quantity) }

            is CartEvent.ItemRemoved -> removeItem(event.productId)

            CartEvent.AddItemClicked -> navigate(CartNavigation.PickProduct)

            // The cart empties when checkout places the order, not here.
            CartEvent.CheckoutClicked -> if (uiState.value.data?.isEmpty == false) navigate(CartNavigation.Checkout)
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        when {
            event is SystemEvent.SnackbarAction && event.id == SNACKBAR_UNDO_REMOVE -> {
                lastRemoved?.let { item -> write { cartRepository.add(item) } }
                lastRemoved = null
            }

            else -> super.onSystemEvent(event)
        }
    }

    /**
     * Adds a product picked in the catalog — the other half of `core.4`'s round trip.
     *
     * [lookup] is passed in rather than injected because it is the catalog's, and this module may
     * not depend on the catalog's presentation. It runs in `viewModelScope`, so an add survives
     * the screen that started it.
     */
    fun onProductPicked(productId: String, lookup: suspend (String) -> CartItem?) = execute(
        action = {
            val item = lookup(productId)
            if (item != null) cartRepository.add(item) else Outcome.Success(Unit)
        },
        onData = {},
    )

    // No overlay: the list is the screen, and one over it on every quantity tap would flash on
    // each press of the stepper.
    private fun observeItems() = observe(
        flow = { cartRepository.observeItems() },
        errorDisplay = ErrorDisplay.Inline,
        onData = { items -> uiState.update { it.copy(data = CartState(items = items)) } },
    )

    private fun removeItem(productId: String) {
        lastRemoved = uiState.value.data?.items?.find { it.productId == productId }
        write { cartRepository.remove(productId) }
        showSnackbar(
            message = R.string.cart_removed.toUiText(),
            actionLabel = R.string.cart_undo.toUiText(),
            id = SNACKBAR_UNDO_REMOVE,
        )
    }

    /**
     * A write whose result the observed flow already reports.
     *
     * `onData` is empty on purpose: the list redraws because the database changed, not because
     * this call returned — two sources of truth for one row is how a cart starts disagreeing with
     * itself.
     */
    private fun write(action: suspend () -> Outcome<Unit>) =
        execute(action = action, onData = {})
}
