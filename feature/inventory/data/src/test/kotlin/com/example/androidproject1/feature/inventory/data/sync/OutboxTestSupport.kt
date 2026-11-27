package com.example.androidproject1.feature.inventory.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.feature.inventory.data.database.InventoryDatabase
import com.example.androidproject1.feature.inventory.data.source.RemoteInventoryDataSource
import com.example.androidproject1.feature.inventory.domain.Item
import com.example.androidproject1.feature.inventory.domain.ItemCategory
import com.example.androidproject1.feature.inventory.domain.ItemCondition

/** An in-memory database, as `InventoryDatabaseTest` builds one. */
internal fun inMemoryInventory(): InventoryDatabase = Room.inMemoryDatabaseBuilder(
    ApplicationProvider.getApplicationContext(),
    InventoryDatabase::class.java,
)
    .setQueryExecutor { it.run() }
    .setTransactionExecutor { it.run() }
    .allowMainThreadQueries()
    .build()

internal fun item(id: String, name: String = "Item $id") = Item(
    id = id,
    name = name,
    category = ItemCategory.Tools,
    condition = ItemCondition.Good,
    quantity = 1,
    priceMinor = 100,
    acquiredOn = null,
    insured = false,
    tags = emptySet(),
    owner = "Ada",
    imageUrl = null,
    notes = "",
)

/** A remote the test scripts: an item id mapped to what its request throws; nothing mapped succeeds. */
internal class ScriptedRemote : RemoteInventoryDataSource {

    val failures = mutableMapOf<String, Throwable>()
    val puts = mutableListOf<String>()
    val deletes = mutableListOf<String>()

    override suspend fun put(item: Item) {
        puts += item.id
        failures[item.id]?.let { throw it }
    }

    override suspend fun delete(itemId: String) {
        deletes += itemId
        failures[itemId]?.let { throw it }
    }
}
