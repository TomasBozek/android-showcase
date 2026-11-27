package com.example.androidproject1.feature.cart.data.repository

import com.example.androidproject1.feature.cart.data.database.CartDatabase
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.withContext

/**
 * The cart feature's share of "delete my account": every table of [CartDatabase].
 *
 * `clearAllTables` is blocking and Room refuses it on the main thread, so it runs on IO here —
 * the same rule the data sources follow, since `BaseRepository` runs on the caller's context.
 */
class DefaultCartEraser(
    private val database: CartDatabase,
    private val dispatcherProvider: DispatcherProvider,
) : LocalDataEraser {

    override suspend fun erase() = withContext(dispatcherProvider.io) {
        database.clearAllTables()
    }
}
