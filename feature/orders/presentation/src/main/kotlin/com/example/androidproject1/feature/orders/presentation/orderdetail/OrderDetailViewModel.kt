package com.example.androidproject1.feature.orders.presentation.orderdetail

import com.example.androidproject1.feature.orders.api.OrderDetailDestination
import com.example.androidproject1.feature.orders.domain.OrdersRepository
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger

/**
 * One kept order, observed — so a refresh that moves its status on moves the timeline while the
 * screen is up — and a silent refresh on open. An id the device keeps nothing for, from a
 * link to an order placed elsewhere, is a message in place of the content; the refresh that runs
 * beside it may bring the order, and the observed flow then draws it.
 */
class OrderDetailViewModel(
    logger: Logger,
    private val ordersRepository: OrdersRepository,
    // The route key, handed in by the destination. Available in `init`, and it comes back with
    // the back stack entry after process death.
    private val args: OrderDetailDestination,
) : BaseViewModel<OrderDetailState, OrderDetailEvent, OrderDetailNavigation>(
    initialState = OrderDetailState(orderId = args.orderId),
    logger = logger.withTag("OrderDetailViewModel"),
) {

    init {
        execute(errorDisplay = ErrorDisplay.Silent, action = { ordersRepository.refresh() }, onData = {})
        observe(
            flow = { ordersRepository.observeOrder(args.orderId) },
            errorDisplay = ErrorDisplay.Inline,
            onData = { order -> updateData { copy(order = order) } },
        )
    }

    override fun onUiEvent(event: OrderDetailEvent) {
        when (event) {
            OrderDetailEvent.UpClicked -> navigate(OrderDetailNavigation.Back)
        }
    }
}
