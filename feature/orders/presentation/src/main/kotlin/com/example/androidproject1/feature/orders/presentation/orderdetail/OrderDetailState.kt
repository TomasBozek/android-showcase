package com.example.androidproject1.feature.orders.presentation.orderdetail

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.orders.domain.Order
import com.example.androidproject1.feature.orders.presentation.orders.OrdersState

/** The order the route names — `null` until the device has read it, or while it keeps none by that id. */
@Immutable
data class OrderDetailState(
    val orderId: String,
    val order: Order? = null,
) {

    companion object {

        val PREVIEW = OrdersState.PREVIEW.orders[1].let { OrderDetailState(orderId = it.id, order = it) }
    }
}
