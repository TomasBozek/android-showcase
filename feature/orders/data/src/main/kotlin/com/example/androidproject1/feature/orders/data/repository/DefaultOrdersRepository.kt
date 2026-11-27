package com.example.androidproject1.feature.orders.data.repository

import com.example.androidproject1.feature.orders.data.source.LocalOrdersDataSource
import com.example.androidproject1.feature.orders.data.source.RemoteOrdersDataSource
import com.example.androidproject1.feature.orders.domain.Order
import com.example.androidproject1.feature.orders.domain.OrdersRepository
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.error.NotFoundError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Orders kept on the device and moved on by the server. The device is where the list is
 * read from, always; the server only adds — an order placed elsewhere, a status reached since.
 */
class DefaultOrdersRepository(
    logger: Logger,
    private val localOrdersDataSource: LocalOrdersDataSource,
    private val remoteOrdersDataSource: RemoteOrdersDataSource,
) : OrdersRepository, BaseRepository(logger = logger.withTag("DefaultOrdersRepository")) {

    override fun observeOrders(): Flow<Outcome<List<Order>>> =
        observe(source = localOrdersDataSource.observeOrders(), retries = RETRIES)

    override fun observeOrder(id: String): Flow<Outcome<Order>> =
        observe(source = localOrdersDataSource.observeOrder(id).map { it ?: throw NotFoundError(message = id) })

    override suspend fun record(order: Order): Outcome<Unit> = execute { localOrdersDataSource.keep(order) }

    override suspend fun refresh(): Outcome<Unit> = execute {
        remoteOrdersDataSource.getOrders().forEach { localOrdersDataSource.keep(it) }
    }

    private companion object {

        const val RETRIES = 3L
    }
}
