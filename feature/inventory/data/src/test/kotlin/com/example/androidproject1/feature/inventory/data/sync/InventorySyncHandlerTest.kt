package com.example.androidproject1.feature.inventory.data.sync

import com.example.androidproject1.feature.inventory.data.database.InventoryDatabase
import com.example.androidproject1.feature.inventory.data.source.DefaultLocalInventoryDataSource
import com.example.androidproject1.feature.inventory.domain.SyncState
import com.example.androidproject1.service.core.domain.error.BadRequestError
import com.example.androidproject1.service.core.domain.error.NotFoundError
import com.example.androidproject1.service.core.domain.error.ServerError
import com.example.androidproject1.service.work.WorkResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Draining the outbox: oldest first; a `2xx` clears an entry, a `5xx` or no network leaves
 * it waiting and asks to be run again, and a refusal — a `409` among them — marks it failed for the
 * user to retry or discard, and the drain carries on past it.
 */
@RunWith(RobolectricTestRunner::class)
class InventorySyncHandlerTest {

    private lateinit var database: InventoryDatabase
    private lateinit var local: DefaultLocalInventoryDataSource
    private val remote = ScriptedRemote()

    @Before
    fun setUp() {
        database = inMemoryInventory()
        local = DefaultLocalInventoryDataSource(database.itemDao(), database.outboxDao(), now = { 1_000L })
    }

    @After
    fun tearDown() = database.close()

    private fun handler() = InventorySyncHandler(local, remote)

    private suspend fun syncOf(id: String): SyncState = local.getItem(id)!!.sync

    @Test
    fun `a 2xx marks the write synced`() = runTest {
        local.upsertItem(item("a"))
        local.enqueue("a", OutboxOp.Put)

        assertEquals(WorkResult.Success, handler().run(emptyMap()))

        assertEquals(listOf("a"), remote.puts)
        assertEquals(SyncState.Synced, syncOf("a"))
    }

    @Test
    fun `a 503 leaves it pending and asks to be run again`() = runTest {
        local.upsertItem(item("a"))
        local.enqueue("a", OutboxOp.Put)
        remote.failures["a"] = ServerError()

        assertEquals(WorkResult.Retry, handler().run(emptyMap()))

        assertEquals(SyncState.Pending, syncOf("a"))
    }

    @Test
    fun `a 409 marks it failed, and the drain carries on past it`() = runTest {
        local.upsertItem(item("a"))
        local.upsertItem(item("b"))
        local.enqueue("a", OutboxOp.Put)
        local.enqueue("b", OutboxOp.Put)
        remote.failures["a"] = BadRequestError("409 Conflict")

        assertEquals(WorkResult.Success, handler().run(emptyMap()))

        assertEquals(SyncState.Failed, syncOf("a"))
        assertEquals(SyncState.Synced, syncOf("b"))
        assertEquals(listOf("a", "b"), remote.puts)
    }

    @Test
    fun `a delete is sent as one, and one the server no longer has is done`() = runTest {
        local.enqueue("gone", OutboxOp.Delete)
        local.enqueue("missing", OutboxOp.Delete)
        remote.failures["missing"] = NotFoundError()

        assertEquals(WorkResult.Success, handler().run(emptyMap()))

        assertEquals(listOf("gone", "missing"), remote.deletes)
        assertEquals(emptyList<String>(), local.pendingWrites().map { it.itemId })
    }
}
