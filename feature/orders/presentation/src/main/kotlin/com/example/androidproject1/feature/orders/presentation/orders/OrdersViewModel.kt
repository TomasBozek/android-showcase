package com.example.androidproject1.feature.orders.presentation.orders

import com.example.androidproject1.feature.orders.domain.OrdersRepository
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger

/**
 * The orders kept on the device, observed, and a refresh on open that moves their statuses on.
 * The refresh is silent: the list is already there, and a server that cannot be reached
 * leaves it as it was rather than raising a dialog over it.
 */
class OrdersViewModel(
    logger: Logger,
    private val ordersRepository: OrdersRepository,
) : BaseViewModel<OrdersState, OrdersEvent, OrdersNavigation>(
    initialState = OrdersState(),
    logger = logger.withTag("OrdersViewModel"),
) {

    init {
        observe(
            flow = { ordersRepository.observeOrders() },
            errorDisplay = ErrorDisplay.Inline,
            onData = { orders -> updateData { copy(orders = orders, loaded = true) } },
        )
        execute(errorDisplay = ErrorDisplay.Silent, action = { ordersRepository.refresh() }, onData = {})
    }

    override fun onUiEvent(event: OrdersEvent) {
        when (event) {
            is OrdersEvent.OrderClicked -> navigate(OrdersNavigation.Detail(event.orderId))
            OrdersEvent.BrowseClicked -> navigate(OrdersNavigation.Catalog)
            OrdersEvent.UpClicked -> navigate(OrdersNavigation.Back)
        }
    }
}
