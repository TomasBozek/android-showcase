package com.example.androidproject1.feature.cart.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.shell.badge
import com.example.androidproject1.core.ui.shell.figure
import com.example.androidproject1.feature.cart.api.CartDestination
import com.example.androidproject1.feature.cart.domain.CartRepository
import com.example.androidproject1.service.core.ui.text.toPluralUiText

/** Cart's door: the Cart tab, its count as the figure and the badge. */
fun cartDoor(repository: CartRepository): ShellDoor = ShellDoor(
    id = "cart",
    section = ShellSection.Shop,
    title = R.string.cart_door_title,
    icon = Icons.Filled.ShoppingCart,
    route = CartDestination,
    placements = setOf(ShellPlacement.Home),
    order = 2,
    figure = repository.observeCount().figure { R.plurals.cart_door_count.toPluralUiText(it, it) },
    badge = repository.observeCount().badge(),
)
