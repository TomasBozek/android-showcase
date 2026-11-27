package com.example.androidproject1.feature.inventory.domain

import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

/** Implemented in the data layer. Declared here so the domain layer depends on nothing. */
interface InventoryRepository {

    fun observeItems(): Flow<Outcome<List<Item>>>

    /** Emits again on every write to this item, and `null` once it is deleted. */
    fun observeItem(itemId: String): Flow<Outcome<Item?>>

    suspend fun getItem(itemId: String): Outcome<Item?>

    suspend fun saveItem(item: Item): Outcome<Unit>

    suspend fun deleteItems(itemIds: Set<String>): Outcome<Unit>

    /**
     * Puts back what [deleteItems] removed, while this process still holds it — the Undo a delete
     * offers. An id not deleted here, or already restored, is skipped.
     */
    suspend fun restoreItems(itemIds: Set<String>): Outcome<Unit>

    /** Puts a [SyncState.Failed] write back in line to be sent. */
    suspend fun retry(itemId: String): Outcome<Unit>

    /** Drops a [SyncState.Failed] write: the item stays as it is here, and the server is not told. */
    suspend fun discard(itemId: String): Outcome<Unit>
}
