package com.example.androidproject1.feature.orders.domain

import java.time.Instant

/**
 * An order the server took, kept on the device. Everything the list and the detail show is
 * here, so neither asks the server; [refresh][OrdersRepository.refresh] only moves [history] on.
 *
 * @property total what was charged, in minor units: the lines, less [discount], plus [deliveryPrice].
 * @property history every status the order has been in, oldest first; never empty.
 */
data class Order(
    val id: String,
    val placedAt: Instant,
    val lines: List<OrderLine>,
    val address: OrderAddress,
    val deliveryName: String,
    val deliveryPrice: Long,
    val discount: Long,
    val total: Long,
    val history: List<StatusChange>,
) {

    /** Where the order is now: the last change in its history. */
    val status: OrderStatus get() = history.last().status
}

/** A line as it was ordered — the name and the price at that moment, not the catalog's today. */
data class OrderLine(
    val productId: String,
    val name: String,
    val price: Long,
    val quantity: Int,
) {

    val lineTotal: Long get() = price * quantity
}

/** Where the order went; `phone` is E.164. */
data class OrderAddress(
    val name: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val phone: String,
)

/**
 * The statuses an order moves through, in order. [Cancelled] can follow any but [Delivered], and
 * nothing follows it.
 */
enum class OrderStatus { Placed, Packed, Shipped, Delivered, Cancelled }

/** One move in an order's history: the status it reached, and when. */
data class StatusChange(
    val status: OrderStatus,
    val at: Instant,
)
