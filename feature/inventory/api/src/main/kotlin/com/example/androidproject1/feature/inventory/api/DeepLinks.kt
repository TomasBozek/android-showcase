package com.example.androidproject1.feature.inventory.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import java.util.UUID

/**
 * `inventory` and `inventory/<id>`: the list, and one item over it. `inventory/new` is
 * the editor on a fresh id, minted per link the way the list mints one — the launcher's New item.
 * An item's id is a UUID, so `new` is never one.
 */
val inventoryDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "inventory" to DeepLinkTable { segments ->
        when (segments.size) {
            0 -> DeepLinkTarget(listOf(InventoryDestination))
            1 -> if (segments[0] == "new") {
                DeepLinkTarget(listOf(InventoryDestination, InventoryEditorDestination(UUID.randomUUID().toString())))
            } else {
                DeepLinkTarget(listOf(InventoryDestination, InventoryDetailDestination(segments[0])))
            }
            else -> null
        }
    },
)
