package com.example.androidproject1.feature.devmenu.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.feature.devmenu.api.DevMenuDestination
import com.example.androidproject1.feature.devmenu.api.DevMenuPlaygroundDestination

/** The debug menu's door, drawn only where the host says the build has one. */
fun devMenuDoor(): ShellDoor = ShellDoor(
    id = "devMenu",
    section = ShellSection.Developer,
    title = R.string.devmenu_door_title,
    icon = Icons.Filled.Info,
    route = DevMenuDestination,
    placements = setOf(ShellPlacement.Home, ShellPlacement.Menu),
    order = 1,
)

/** The component playground's door, beside the debug menu's. */
fun playgroundDoor(): ShellDoor = ShellDoor(
    id = "playground",
    section = ShellSection.Developer,
    title = R.string.devmenu_door_playground_title,
    icon = Icons.Filled.Create,
    route = DevMenuPlaygroundDestination(),
    placements = setOf(ShellPlacement.Home, ShellPlacement.Menu),
    order = 3,
)
