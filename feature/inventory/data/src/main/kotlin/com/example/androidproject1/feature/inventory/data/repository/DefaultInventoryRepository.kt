package com.example.androidproject1.feature.inventory.data.repository

import com.example.androidproject1.feature.inventory.data.source.LocalInventoryDataSource
import com.example.androidproject1.feature.inventory.data.sync.InventorySyncHandler
import com.example.androidproject1.feature.inventory.data.sync.OutboxOp
import com.example.androidproject1.feature.inventory.domain.InventoryRepository
import com.example.androidproject1.feature.inventory.domain.Item
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.work.NetworkRequirement
import com.example.androidproject1.service.work.Scheduler
import com.example.androidproject1.service.work.WorkConstraints
import com.example.androidproject1.service.work.WorkSpec
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

/**
 * Every write lands in Room at once and waits in the outbox: the screens show it straight
 * away, marked pending, and [InventorySyncHandler] sends it when there is a network.
 */
class DefaultInventoryRepository(
    logger: Logger,
    private val localInventoryDataSource: LocalInventoryDataSource,
    private val scheduler: Scheduler,
) : InventoryRepository, BaseRepository(logger = logger.withTag("DefaultInventoryRepository")) {

    // retries, because the list and the detail hold this collector for as long as the screen
    // exists — one transient database error must not stop it emitting for good.
    override fun observeItems(): Flow<Outcome<List<Item>>> =
        observe(source = localInventoryDataSource.observeItems(), retries = RETRIES)

    override fun observeItem(itemId: String): Flow<Outcome<Item?>> =
        observe(source = localInventoryDataSource.observeItem(itemId), retries = RETRIES)

    override suspend fun getItem(itemId: String): Outcome<Item?> = execute { localInventoryDataSource.getItem(itemId) }

    override suspend fun saveItem(item: Item): Outcome<Unit> = execute {
        localInventoryDataSource.upsertItem(item)
        localInventoryDataSource.enqueue(item.id, OutboxOp.Put)
        requestSync()
    }

    // What the last deletes removed, kept for their Undo. Memory, not disk: an Undo is offered for
    // seconds, and the message offering it does not outlive the process either.
    private val deleted = ConcurrentHashMap<String, Item>()

    override suspend fun deleteItems(itemIds: Set<String>): Outcome<Unit> = execute {
        itemIds.forEach { id -> localInventoryDataSource.getItem(id)?.let { deleted[id] = it } }
        localInventoryDataSource.deleteItems(itemIds)
        itemIds.forEach { localInventoryDataSource.enqueue(it, OutboxOp.Delete) }
        requestSync()
    }

    override suspend fun restoreItems(itemIds: Set<String>): Outcome<Unit> = execute {
        itemIds.forEach { id ->
            deleted.remove(id)?.let {
                localInventoryDataSource.upsertItem(it)
                localInventoryDataSource.enqueue(id, OutboxOp.Put)
            }
        }
        requestSync()
    }

    override suspend fun retry(itemId: String): Outcome<Unit> = execute {
        localInventoryDataSource.markPending(itemId)
        requestSync()
    }

    override suspend fun discard(itemId: String): Outcome<Unit> = execute {
        localInventoryDataSource.clearWrite(itemId)
    }

    /** One unique work, run once there is a network; a second ask while one waits is the same run. */
    private fun requestSync() = scheduler.schedule(
        WorkSpec(
            name = InventorySyncHandler.KEY,
            handler = InventorySyncHandler.KEY,
            constraints = WorkConstraints(network = NetworkRequirement.Connected),
            replace = false,
        ),
    )

    private companion object {

        const val RETRIES = 3L
    }
}
