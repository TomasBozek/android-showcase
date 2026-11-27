package com.example.androidproject1.feature.orders.presentation.orders

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.orders.domain.Order
import com.example.androidproject1.feature.orders.domain.OrderAddress
import com.example.androidproject1.feature.orders.domain.OrderLine
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.domain.StatusChange
import java.time.Instant

/**
 * Every kept order, newest first, and whether the first read has come back — so an empty list is
 * told from one not read yet. [sections] is what the screen draws: one group per status, in the
 * order an order moves, newest first inside each.
 */
@Immutable
data class OrdersState(
    val orders: List<Order> = emptyList(),
    val loaded: Boolean = false,
) {

    val sections: List<OrdersSection>
        get() = OrderStatus.entries.mapNotNull { status ->
            orders.filter { it.status == status }.takeIf { it.isNotEmpty() }?.let { OrdersSection(status, it) }
        }

    val isEmpty: Boolean get() = loaded && orders.isEmpty()

    companion object {

        private fun order(id: String, status: OrderStatus, placedAt: String, total: Long): Order {
            val at = Instant.parse(placedAt)
            return Order(
                id = id,
                placedAt = at,
                lines = listOf(OrderLine("coffee", "Coffee", 450, 2)),
                address = OrderAddress("Ada Lovelace", "Na Příkopě 1", "Praha", "110 00", "CZ", "+420777123456"),
                deliveryName = "Standard courier",
                deliveryPrice = 490,
                discount = 0,
                total = total,
                history = OrderStatus.entries.filter { it <= status && it != OrderStatus.Cancelled }
                    .ifEmpty { listOf(OrderStatus.Placed) }
                    .map { StatusChange(it, at) } +
                    listOfNotNull(
                        StatusChange(OrderStatus.Cancelled, at).takeIf {
                            status == OrderStatus.Cancelled
                        },
                    ),
            )
        }

        val PREVIEW = OrdersState(
            orders = listOf(
                order("A10001", OrderStatus.Placed, "2026-09-28T10:00:00Z", 1_390),
                order("A10000", OrderStatus.Shipped, "2026-09-25T10:00:00Z", 2_430),
                order("Z0DLV1", OrderStatus.Delivered, "2026-09-16T10:00:00Z", 1_840),
                order("Z0CNL1", OrderStatus.Cancelled, "2026-08-29T10:00:00Z", 600),
            ),
            loaded = true,
        )
    }
}

/** One status's group in the list. */
@Immutable
data class OrdersSection(val status: OrderStatus, val orders: List<Order>)
