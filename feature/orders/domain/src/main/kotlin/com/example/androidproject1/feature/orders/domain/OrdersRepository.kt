package com.example.androidproject1.feature.orders.domain

import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

/** Implemented in the data layer. Declared here so the domain layer depends on nothing. */
interface OrdersRepository {

    /** Every order kept, newest first. */
    fun observeOrders(): Flow<Outcome<List<Order>>>

    /** One order, or `Failure(NotFoundError)` while none by that id is kept. */
    fun observeOrder(id: String): Flow<Outcome<Order>>

    /** Keeps an order checkout just placed, so it is listed before the server is asked again. */
    suspend fun record(order: Order): Outcome<Unit>

    /**
     * Asks the server (`GET orders`) and keeps what it says: an order not kept yet is added, a
     * status change not kept yet is appended. Nothing kept is removed, so an order placed here is
     * never lost to a server that has not caught up.
     */
    suspend fun refresh(): Outcome<Unit>
}
