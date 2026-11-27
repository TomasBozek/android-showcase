package com.example.androidproject1.feature.gallery.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.feature.gallery.api.GalleryDestination
import com.example.androidproject1.feature.gallery.api.GalleryDetailDestination

/** The component gallery's door, drawn only where the host says the build has one. */
fun galleryDoor(): ShellDoor = ShellDoor(
    id = "gallery",
    section = ShellSection.Developer,
    title = R.string.gallery_door_title,
    icon = Icons.Filled.Star,
    route = GalleryDestination,
    placements = setOf(ShellPlacement.Home, ShellPlacement.Menu),
    order = 2,
)

/**
 * The screens of gallery the debug menu opens in one tap; a route with an argument gets a
 * value the fixtures serve on every flavor.
 */
val galleryJumps: List<ShellJump> = listOf(
    ShellJump(id = "gallery", screenId = "GalleryScreen", route = GalleryDestination),
    ShellJump(
        id = "galleryDetail",
        screenId = "GalleryDetailScreen",
        route = GalleryDetailDestination(componentId = "button"),
    ),
)
