package com.example.androidproject1.feature.orders.data.source

import com.example.androidproject1.feature.orders.domain.Order
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.network.HttpErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.withContext

/** On IO, and every failure a `DomainError` before it leaves. */
class DefaultRemoteOrdersDataSource(
    private val client: HttpClient,
    private val dispatcherProvider: DispatcherProvider,
) : RemoteOrdersDataSource {

    override suspend fun getOrders(): List<Order> = withContext(dispatcherProvider.io) {
        try {
            client.get("orders").body<List<OrderDto>>().map { it.toDomain() }
        } catch (throwable: Throwable) {
            throw HttpErrorMapper.map(throwable)
        }
    }
}
