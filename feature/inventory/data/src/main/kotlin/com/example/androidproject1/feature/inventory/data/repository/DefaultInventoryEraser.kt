package com.example.androidproject1.feature.inventory.data.repository

import com.example.androidproject1.feature.inventory.data.database.InventoryDatabase
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.withContext

/**
 * The inventory feature's share of "delete my account": every table of [InventoryDatabase].
 *
 * `clearAllTables` is blocking and Room refuses it on the main thread, so it runs on IO here —
 * the same rule the data sources follow, since `BaseRepository` runs on the caller's context.
 */
class DefaultInventoryEraser(
    private val database: InventoryDatabase,
    private val dispatcherProvider: DispatcherProvider,
) : LocalDataEraser {

    override suspend fun erase() = withContext(dispatcherProvider.io) {
        database.clearAllTables()
    }
}
