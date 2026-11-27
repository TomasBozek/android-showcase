package com.example.androidproject1.feature.inventory.data.sync

import com.example.androidproject1.feature.inventory.data.source.LocalInventoryDataSource
import com.example.androidproject1.feature.inventory.data.source.RemoteInventoryDataSource
import com.example.androidproject1.service.core.domain.error.NetworkError
import com.example.androidproject1.service.core.domain.error.NotFoundError
import com.example.androidproject1.service.core.domain.error.RateLimitedError
import com.example.androidproject1.service.core.domain.error.ServerError
import com.example.androidproject1.service.core.domain.error.UnauthorizedError
import com.example.androidproject1.service.work.WorkHandler
import com.example.androidproject1.service.work.WorkResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Drains Inventory's outbox, oldest first. A put sends the item as it is in Room *now* — the
 * row is the truth, the outbox only says it changed. A `2xx` clears the write; no network, a
 * `5xx`, a `429` or an expired session stop the drain and ask WorkManager to run it again with its
 * backoff, the write still waiting; any other refusal — a `409` among them — marks the write
 * failed for the user to retry or discard, and the drain carries on past it. A delete the server no
 * longer has is done.
 */
class InventorySyncHandler(
    private val local: LocalInventoryDataSource,
    private val remote: RemoteInventoryDataSource,
) : WorkHandler {

    override val key: String = KEY

    override suspend fun run(input: Map<String, String>): WorkResult {
        for (write in local.pendingWrites()) {
            try {
                when (write.op) {
                    OutboxOp.Put -> local.getItem(write.itemId)?.let { remote.put(it) }
                    OutboxOp.Delete -> remote.delete(write.itemId)
                }
                local.clearWrite(write.itemId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: NetworkError) {
                return WorkResult.Retry
            } catch (_: ServerError) {
                return WorkResult.Retry
            } catch (_: RateLimitedError) {
                return WorkResult.Retry
            } catch (_: UnauthorizedError) {
                return WorkResult.Retry
            } catch (notFound: NotFoundError) {
                if (write.op == OutboxOp.Delete) local.clearWrite(write.itemId) else local.markFailed(write.itemId)
            } catch (_: Throwable) {
                local.markFailed(write.itemId)
            }
        }
        return WorkResult.Success
    }

    companion object {

        const val KEY = "inventory-sync"
    }
}
