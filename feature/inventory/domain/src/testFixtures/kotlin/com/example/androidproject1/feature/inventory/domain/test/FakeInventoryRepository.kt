package com.example.androidproject1.feature.inventory.domain.test

import com.example.androidproject1.feature.inventory.domain.InventoryRepository
import com.example.androidproject1.feature.inventory.domain.Item
import com.example.androidproject1.feature.inventory.domain.ItemCategory
import com.example.androidproject1.feature.inventory.domain.ItemCondition
import com.example.androidproject1.feature.inventory.domain.ItemTag
import com.example.androidproject1.feature.inventory.domain.SyncState
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [InventoryRepository], backed by a `MutableStateFlow` so a test that saves or deletes
 * sees an observing collector react the way Room would.
 */
class FakeInventoryRepository(
    initial: List<Item> = listOf(DRILL, KETTLE),
    var failWith: DomainError? = null,
) : InventoryRepository {

    private val items = MutableStateFlow(initial)

    val current: List<Item> get() = items.value

    var saved: Item? = null
        private set

    var deletedIds: Set<String> = emptySet()
        private set

    override fun observeItems(): Flow<Outcome<List<Item>>> =
        items.map { failWith?.let { Outcome.Failure(it) } ?: Outcome.Success(it) }

    override fun observeItem(itemId: String): Flow<Outcome<Item?>> =
        items.map { list -> failWith?.let { Outcome.Failure(it) } ?: Outcome.Success(list.find { it.id == itemId }) }

    override suspend fun getItem(itemId: String): Outcome<Item?> {
        failWith?.let { return Outcome.Failure(it) }
        return Outcome.Success(items.value.find { it.id == itemId })
    }

    override suspend fun saveItem(item: Item): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        saved = item
        items.value = items.value.filterNot { it.id == item.id } + item
        return Outcome.Success(Unit)
    }

    override suspend fun deleteItems(itemIds: Set<String>): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        deletedIds = itemIds
        removed += items.value.filter { it.id in itemIds }
        items.value = items.value.filterNot { it.id in itemIds }
        return Outcome.Success(Unit)
    }

    private val removed = mutableListOf<Item>()

    override suspend fun restoreItems(itemIds: Set<String>): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        val back = removed.filter { it.id in itemIds }
        removed -= back.toSet()
        items.value = items.value + back
        return Outcome.Success(Unit)
    }

    /** The ids [retry] and [discard] were called with, in order. */
    val retried = mutableListOf<String>()
    val discarded = mutableListOf<String>()

    override suspend fun retry(itemId: String): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        retried += itemId
        items.value = items.value.map { if (it.id == itemId) it.copy(sync = SyncState.Pending) else it }
        return Outcome.Success(Unit)
    }

    override suspend fun discard(itemId: String): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        discarded += itemId
        items.value = items.value.map { if (it.id == itemId) it.copy(sync = SyncState.Synced) else it }
        return Outcome.Success(Unit)
    }

    companion object {

        val DRILL = Item(
            id = "item-drill",
            name = "Cordless drill",
            category = ItemCategory.Tools,
            condition = ItemCondition.Good,
            quantity = 1,
            priceMinor = 2_490_00,
            acquiredOn = null,
            insured = false,
            tags = setOf(ItemTag.Lent),
            owner = "Jana Nováková",
            imageUrl = "https://example.com/images/drill.jpg",
            notes = "Lent to Petr until the shelves are up.",
            barcode = "8590001000014",
        )

        val KETTLE = Item(
            id = "item-kettle",
            name = "Electric kettle",
            category = ItemCategory.Electronics,
            condition = ItemCondition.New,
            quantity = 1,
            priceMinor = 890_00,
            acquiredOn = null,
            insured = false,
            tags = setOf(ItemTag.Favourite),
            owner = "Petr Svoboda",
            imageUrl = null,
            notes = "",
        )
    }
}
