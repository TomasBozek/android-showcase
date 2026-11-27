package com.example.androidproject1.feature.inventory.data.repository

import com.example.androidproject1.feature.inventory.data.database.InventoryDatabase
import com.example.androidproject1.feature.inventory.data.source.DefaultLocalInventoryDataSource
import com.example.androidproject1.feature.inventory.data.sync.InventorySyncHandler
import com.example.androidproject1.feature.inventory.data.sync.OutboxOp
import com.example.androidproject1.feature.inventory.data.sync.inMemoryInventory
import com.example.androidproject1.feature.inventory.data.sync.item
import com.example.androidproject1.feature.inventory.domain.SyncState
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.work.NetworkRequirement
import com.example.androidproject1.service.work.test.FakeScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A write lands in Room at once and waits in the outbox: the screen shows it straight away,
 * marked pending, and the sync work is asked for — constrained to a network.
 */
@RunWith(RobolectricTestRunner::class)
class DefaultInventoryRepositoryTest {

    private lateinit var database: InventoryDatabase
    private lateinit var local: DefaultLocalInventoryDataSource
    private val scheduler = FakeScheduler()

    @Before
    fun setUp() {
        database = inMemoryInventory()
        local = DefaultLocalInventoryDataSource(database.itemDao(), database.outboxDao(), now = { 1_000L })
    }

    @After
    fun tearDown() = database.close()

    private fun repository() = DefaultInventoryRepository(FakeLogger(), local, scheduler)

    @Test
    fun `a save is in Room and pending at once, and the sync is asked for`() = runTest {
        repository().saveItem(item("a"))

        assertEquals(SyncState.Pending, local.getItem("a")!!.sync)
        val spec = scheduler.scheduled.single()
        assertEquals(InventorySyncHandler.KEY, spec.handler)
        assertEquals(NetworkRequirement.Connected, spec.constraints.network)
    }

    @Test
    fun `a delete waits in the outbox as a delete`() = runTest {
        local.upsertItem(item("a"))

        repository().deleteItems(setOf("a"))

        assertEquals(listOf("a" to OutboxOp.Delete), local.pendingWrites().map { it.itemId to it.op })
        assertTrue(scheduler.scheduled.isNotEmpty())
    }

    @Test
    fun `retry puts a failed write back in line, discard drops it`() = runTest {
        local.upsertItem(item("a"))
        local.upsertItem(item("b"))
        local.enqueue("a", OutboxOp.Put)
        local.enqueue("b", OutboxOp.Put)
        local.markFailed("a")
        local.markFailed("b")
        val repository = repository()

        assertTrue(repository.retry("a") is Outcome.Success)
        assertTrue(repository.discard("b") is Outcome.Success)

        assertEquals(SyncState.Pending, local.getItem("a")!!.sync)
        assertEquals(SyncState.Synced, local.getItem("b")!!.sync)
        assertEquals(1, scheduler.scheduled.size)
    }
}
