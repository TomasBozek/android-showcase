package com.example.androidproject1.feature.inventory.data.source

import com.example.androidproject1.feature.inventory.data.sync.OutboxEntry
import com.example.androidproject1.feature.inventory.data.sync.OutboxOp
import com.example.androidproject1.feature.inventory.domain.Item
import kotlinx.coroutines.flow.Flow

/**
 * Internal to the data layer: what the rest of the app depends on is `InventoryRepository` in
 * `domain`. The items and their outbox are one database, so they are one source: an item
 * reads back with the [Item.sync] its waiting write gives it.
 */
interface LocalInventoryDataSource {

    fun observeItems(): Flow<List<Item>>

    fun observeItem(itemId: String): Flow<Item?>

    suspend fun getItem(itemId: String): Item?

    suspend fun upsertItem(item: Item)

    suspend fun deleteItems(itemIds: Set<String>)

    /** Puts [op] in line for [itemId], replacing whatever of its writes was waiting. */
    suspend fun enqueue(itemId: String, op: OutboxOp)

    /** What the drain sends, oldest first; failed writes wait for the user. */
    suspend fun pendingWrites(): List<OutboxEntry>

    suspend fun markFailed(itemId: String)

    /** A failed write back in line. */
    suspend fun markPending(itemId: String)

    /** The write is done with — accepted, or discarded. */
    suspend fun clearWrite(itemId: String)
}
