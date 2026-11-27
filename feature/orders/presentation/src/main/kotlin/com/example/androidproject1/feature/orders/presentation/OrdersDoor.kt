package com.example.androidproject1.feature.orders.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.shell.figure
import com.example.androidproject1.feature.orders.api.OrdersDestination
import com.example.androidproject1.feature.orders.domain.OrdersRepository
import com.example.androidproject1.service.core.ui.text.toPluralUiText

/** Orders's door: the orders kept on the device; Home counts them and does not refresh, which is the list's job. */
fun ordersDoor(repository: OrdersRepository): ShellDoor = ShellDoor(
    id = "orders",
    section = ShellSection.Shop,
    title = R.string.orders_door_title,
    icon = Icons.AutoMirrored.Filled.Send,
    route = OrdersDestination,
    placements = setOf(ShellPlacement.Home, ShellPlacement.Profile),
    order = 3,
    figure = repository.observeOrders().figure { R.plurals.orders_door_count.toPluralUiText(it.size, it.size) },
)
