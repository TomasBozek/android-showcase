package com.example.androidproject1.feature.inventory.data.source

import com.example.androidproject1.feature.inventory.data.database.ItemDao
import com.example.androidproject1.feature.inventory.data.database.OutboxDao
import com.example.androidproject1.feature.inventory.data.database.OutboxEntity
import com.example.androidproject1.feature.inventory.data.database.toDomain
import com.example.androidproject1.feature.inventory.data.database.toEntity
import com.example.androidproject1.feature.inventory.data.sync.OutboxEntry
import com.example.androidproject1.feature.inventory.data.sync.OutboxOp
import com.example.androidproject1.feature.inventory.domain.Item
import com.example.androidproject1.feature.inventory.domain.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Seeds [ITEM_FIXTURES] into the table the first time anything asks, exactly as
 * `DefaultLocalDestinationsDataSource` does: before subscribing to the query, because Room's
 * Flow only replays what the table held at subscription time, and a seed from inside a `map`
 * would let the first collection through empty. `INSERT OR IGNORE`, so an item the user has
 * edited or deleted is never put back — the fixtures are a starting point, not a refresh.
 *
 * An item reads back with the [Item.sync] its outbox row gives it: the two tables are
 * combined here, so the screens see a write's state without knowing there is an outbox.
 */
class DefaultLocalInventoryDataSource(
    private val itemDao: ItemDao,
    private val outboxDao: OutboxDao,
    private val now: () -> Long = System::currentTimeMillis,
) : LocalInventoryDataSource {

    private var seeded = false

    override fun observeItems(): Flow<List<Item>> = flow {
        ensureSeeded()
        emitAll(
            combine(itemDao.observeItems(), outboxDao.observeAll()) { rows, outbox ->
                val writes = outbox.associateBy { it.itemId }
                rows.map { it.toDomain().withSync(writes[it.id]) }
            },
        )
    }

    override fun observeItem(itemId: String): Flow<Item?> = flow {
        ensureSeeded()
        emitAll(
            combine(itemDao.observeItem(itemId), outboxDao.observeAll()) { row, outbox ->
                row?.toDomain()?.withSync(outbox.firstOrNull { it.itemId == itemId })
            },
        )
    }

    override suspend fun getItem(itemId: String): Item? {
        ensureSeeded()
        return itemDao.getItem(itemId)?.toDomain()?.withSync(outboxDao.get(itemId))
    }

    override suspend fun upsertItem(item: Item) = itemDao.upsert(item.toEntity())

    override suspend fun deleteItems(itemIds: Set<String>) = itemDao.delete(itemIds)

    override suspend fun enqueue(itemId: String, op: OutboxOp) =
        outboxDao.upsert(OutboxEntity(itemId = itemId, op = op.name, failed = false, enqueuedAt = now()))

    override suspend fun pendingWrites(): List<OutboxEntry> = outboxDao.pending().map { it.toEntry() }

    override suspend fun markFailed(itemId: String) = outboxDao.setFailed(itemId, failed = true)

    override suspend fun markPending(itemId: String) = outboxDao.setFailed(itemId, failed = false)

    override suspend fun clearWrite(itemId: String) = outboxDao.delete(itemId)

    private suspend fun ensureSeeded() {
        if (seeded) return
        if (itemDao.count() == 0) {
            itemDao.insertAll(ITEM_FIXTURES.map { it.toEntity() })
        }
        seeded = true
    }

    private fun Item.withSync(write: OutboxEntity?): Item = copy(
        sync = when {
            write == null -> SyncState.Synced
            write.failed -> SyncState.Failed
            else -> SyncState.Pending
        },
    )

    private fun OutboxEntity.toEntry() = OutboxEntry(itemId = itemId, op = OutboxOp.valueOf(op), failed = failed)
}
