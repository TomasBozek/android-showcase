package com.example.androidproject1.feature.inventory.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import com.example.androidproject1.core.ui.shell.ShellCreation
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.shell.figure
import com.example.androidproject1.feature.inventory.api.InventoryDestination
import com.example.androidproject1.feature.inventory.api.InventoryDetailDestination
import com.example.androidproject1.feature.inventory.api.InventoryEditorDestination
import com.example.androidproject1.feature.inventory.domain.InventoryRepository
import com.example.androidproject1.service.core.ui.text.toPluralUiText
import java.util.UUID

/** Inventory's door: the items kept. */
fun inventoryDoor(repository: InventoryRepository): ShellDoor = ShellDoor(
    id = "inventory",
    section = ShellSection.Yours,
    title = R.string.inventory_door_title,
    icon = Icons.Filled.Build,
    route = InventoryDestination,
    placements = setOf(ShellPlacement.Home, ShellPlacement.Menu),
    order = 2,
    figure = repository.observeItems().figure { R.plurals.inventory_door_count.toPluralUiText(it.size, it.size) },
)

/**
 * Inventory's choice in Home's speed dial. A new item: the editor on an id minted on
 * each press, the way the list mints one — the editor edits whatever id it is given.
 */
fun itemCreation(): ShellCreation = ShellCreation(
    id = "item",
    label = R.string.inventory_create_title,
    icon = Icons.Filled.Build,
    testTag = "home_newItemItem",
    order = 1,
) { InventoryEditorDestination(UUID.randomUUID().toString()) }

/**
 * The screens of inventory the debug menu opens in one tap; a route with an argument gets a
 * value the fixtures serve on every flavor.
 */
val inventoryJumps: List<ShellJump> = listOf(
    ShellJump(
        id = "inventoryDetail",
        screenId = "InventoryDetailScreen",
        route = InventoryDetailDestination(itemId = "item-drill"),
    ),
    ShellJump(
        id = "inventoryEditor",
        screenId = "InventoryEditorScreen",
        route = InventoryEditorDestination(itemId = "item-drill"),
    ),
)
