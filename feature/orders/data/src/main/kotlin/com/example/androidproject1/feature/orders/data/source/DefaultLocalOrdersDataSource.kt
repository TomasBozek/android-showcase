package com.example.androidproject1.feature.orders.data.source

import com.example.androidproject1.feature.orders.data.database.OrdersDao
import com.example.androidproject1.feature.orders.data.database.historyEntities
import com.example.androidproject1.feature.orders.data.database.toDomain
import com.example.androidproject1.feature.orders.data.database.toEntity
import com.example.androidproject1.feature.orders.domain.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultLocalOrdersDataSource(private val ordersDao: OrdersDao) : LocalOrdersDataSource {

    override fun observeOrders(): Flow<List<Order>> = ordersDao.observeOrders().map { rows ->
        rows.map { it.toDomain() }
    }

    override fun observeOrder(id: String): Flow<Order?> = ordersDao.observeOrder(id).map { it?.toDomain() }

    override suspend fun keep(order: Order) = ordersDao.keep(order.toEntity(), order.historyEntities())
}
