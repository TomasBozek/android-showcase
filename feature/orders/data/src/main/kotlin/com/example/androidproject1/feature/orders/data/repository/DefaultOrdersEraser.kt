package com.example.androidproject1.feature.orders.data.repository

import com.example.androidproject1.feature.orders.data.database.OrdersDatabase
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.withContext

/** The orders feature's share of "delete my account": every table of [OrdersDatabase], on IO. */
class DefaultOrdersEraser(
    private val database: OrdersDatabase,
    private val dispatcherProvider: DispatcherProvider,
) : LocalDataEraser {

    override suspend fun erase() = withContext(dispatcherProvider.io) {
        database.clearAllTables()
    }
}
