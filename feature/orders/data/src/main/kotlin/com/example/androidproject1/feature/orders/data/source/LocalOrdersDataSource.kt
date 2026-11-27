package com.example.androidproject1.feature.orders.data.source

import com.example.androidproject1.feature.orders.domain.Order
import kotlinx.coroutines.flow.Flow

/** The orders kept on the device. */
interface LocalOrdersDataSource {

    fun observeOrders(): Flow<List<Order>>

    fun observeOrder(id: String): Flow<Order?>

    /** Adds the order if it is not kept, and every status change of it that is not. */
    suspend fun keep(order: Order)
}
