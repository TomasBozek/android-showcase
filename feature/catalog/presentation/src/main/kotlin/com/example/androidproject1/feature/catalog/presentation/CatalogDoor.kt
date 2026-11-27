package com.example.androidproject1.feature.catalog.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.shell.figure
import com.example.androidproject1.feature.catalog.api.CategoriesDestination
import com.example.androidproject1.feature.catalog.api.ProductSearchDestination
import com.example.androidproject1.feature.catalog.domain.CatalogRepository
import com.example.androidproject1.service.core.ui.text.toPluralUiText

/** Catalog's door: the Catalog tab, counting its categories. */
fun catalogDoor(repository: CatalogRepository): ShellDoor = ShellDoor(
    id = "catalog",
    section = ShellSection.Shop,
    title = R.string.catalog_door_title,
    icon = Icons.AutoMirrored.Filled.List,
    route = CategoriesDestination,
    placements = setOf(ShellPlacement.Home),
    order = 1,
    figure = repository.observeCategories().figure { R.plurals.catalog_door_count.toPluralUiText(it.size, it.size) },
)

/**
 * The screens of catalog the debug menu opens in one tap; a route with an argument gets a
 * value the fixtures serve on every flavor.
 */
val catalogJumps: List<ShellJump> = listOf(
    ShellJump(id = "productSearch", screenId = "ProductSearchScreen", route = ProductSearchDestination),
)
