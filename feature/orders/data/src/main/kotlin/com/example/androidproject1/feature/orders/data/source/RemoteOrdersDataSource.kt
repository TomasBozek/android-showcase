package com.example.androidproject1.feature.orders.data.source

import com.example.androidproject1.feature.orders.domain.Order

/** The server's orders for the signed-in account. */
interface RemoteOrdersDataSource {

    suspend fun getOrders(): List<Order>
}
