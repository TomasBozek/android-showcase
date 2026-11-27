package com.example.androidproject1.feature.inventory.presentation.message

import com.example.androidproject1.feature.inventory.domain.InventoryRepository
import com.example.androidproject1.service.core.ui.event.MessageActionHandler

/**
 * The Undo on "Item deleted". The detail that deleted the item has closed by the time the
 * message is read on the list, so the action is this handler, bound in `InventoryModule` under
 * [KEY] and given the item's id — never a lambda into a view model that is gone.
 */
class InventoryRestoreHandler(private val inventoryRepository: InventoryRepository) : MessageActionHandler {

    override val key: String = KEY

    override suspend fun handle(argument: String) {
        inventoryRepository.restoreItems(setOf(argument))
    }

    companion object {

        const val KEY = "inventory.restore"
    }
}
