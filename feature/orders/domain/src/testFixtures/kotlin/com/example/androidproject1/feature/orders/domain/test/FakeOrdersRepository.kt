package com.example.androidproject1.feature.orders.domain.test

import com.example.androidproject1.feature.orders.domain.Order
import com.example.androidproject1.feature.orders.domain.OrderAddress
import com.example.androidproject1.feature.orders.domain.OrderLine
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.domain.OrdersRepository
import com.example.androidproject1.feature.orders.domain.StatusChange
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.error.NotFoundError
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** In-memory [OrdersRepository], beside the interface it fakes. [refreshed] counts the refreshes. */
class FakeOrdersRepository(
    initial: List<Order> = emptyList(),
    var failWith: DomainError? = null,
) : OrdersRepository {

    private val orders = MutableStateFlow(initial)

    val current: List<Order> get() = orders.value

    var refreshed: Int = 0
        private set

    override fun observeOrders(): Flow<Outcome<List<Order>>> =
        orders.map { list ->
            failWith?.let { Outcome.Failure(it) }
                ?: Outcome.Success(list.sortedByDescending { it.placedAt })
        }

    override fun observeOrder(id: String): Flow<Outcome<Order>> = orders.map { list ->
        failWith?.let { return@map Outcome.Failure(it) }
        list.firstOrNull { it.id == id }?.let { Outcome.Success(it) } ?: Outcome.Failure(NotFoundError(message = id))
    }

    override suspend fun record(order: Order): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        orders.value = orders.value.filterNot { it.id == order.id } + order
        return Outcome.Success(Unit)
    }

    override suspend fun refresh(): Outcome<Unit> {
        refreshed += 1
        return failWith?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)
    }

    companion object {

        /** An order in [status], its history every status up to it, an hour apart. */
        fun order(
            id: String = "A1",
            status: OrderStatus = OrderStatus.Placed,
            placedAt: Instant = Instant.parse("2026-09-28T10:00:00Z"),
        ): Order {
            val reached = OrderStatus.entries.filter { it != OrderStatus.Cancelled }.takeWhile { it <= status }
                .ifEmpty { listOf(OrderStatus.Placed) }
                .let { if (status == OrderStatus.Cancelled) listOf(OrderStatus.Placed, OrderStatus.Cancelled) else it }
            return Order(
                id = id,
                placedAt = placedAt,
                lines = listOf(OrderLine(productId = "coffee", name = "Coffee", price = 450, quantity = 2)),
                address = OrderAddress("Ada Lovelace", "Na Příkopě 1", "Praha", "110 00", "CZ", "+420777123456"),
                deliveryName = "Standard courier",
                deliveryPrice = 490,
                discount = 0,
                total = 1390,
                history = reached.mapIndexed { index, it -> StatusChange(it, placedAt.plusSeconds(HOUR * index)) },
            )
        }

        private const val HOUR = 3_600L
    }
}
