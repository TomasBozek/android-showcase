package com.example.androidproject1.feature.home.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.feature.home.presentation.HomeCreationState
import com.example.androidproject1.feature.home.presentation.HomeDoorState
import com.example.androidproject1.feature.home.presentation.R
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText

@Immutable
data class HomeState(
    // UiText rather than String: the ViewModel picks the text without a Context.
    val greeting: UiText,
    /** The contributed doors placed on Home, by section and order; figures fill in as they arrive. */
    val doors: List<HomeDoorState> = emptyList(),
    /** Whether this build has the developer screens to open — reported by the host, not assumed. */
    val developerDoors: Boolean = false,
    /** What the speed dial offers, each a feature's contribution, in order. */
    val creations: List<HomeCreationState> = emptyList(),
    /** The speed dial is open; state rather than a remember, so a rotation keeps it and a test reads it. */
    val createOpen: Boolean = false,
) {

    /** Every public door has its figure — the moment Home reports the activity fully drawn. */
    val figuresIn: Boolean get() = doors.filter { it.section != ShellSection.Developer }.all { it.figure != null }

    /** Whether there is a dial at all: hidden when no feature contributes something to make. */
    val canCreate: Boolean get() = creations.isNotEmpty()

    /** A door's current state, or `null` for an id Home was not given. */
    fun door(id: String): HomeDoorState? = doors.firstOrNull { it.id == id }

    companion object {

        val PREVIEW = HomeState(
            greeting = R.string.home_greeting_named.toUiText("Ada"),
            doors = listOf(
                door("catalog", ShellSection.Shop, "Catalog", Icons.AutoMirrored.Filled.List, "4 categories"),
                door("cart", ShellSection.Shop, "Cart", Icons.Filled.ShoppingCart, "2 items", badge = 2),
                door("orders", ShellSection.Shop, "Your orders", Icons.AutoMirrored.Filled.Send, "3 orders"),
                door("trips", ShellSection.Yours, "Trips", Icons.Filled.DateRange, "1 trip"),
                door("inventory", ShellSection.Yours, "Inventory", Icons.Filled.Build, "12 items kept"),
                door("movies", ShellSection.Yours, "Shows", Icons.Filled.PlayArrow, "3 favourites"),
                door("weather", ShellSection.Yours, "Weather", Icons.Filled.LocationOn, "2 places"),
                door("profile", ShellSection.App, "Profile", Icons.Filled.Person, "ada@example.com"),
                door("settings", ShellSection.App, "Settings", Icons.Filled.Settings, "Theme follows the device"),
                door("devMenu", ShellSection.Developer, "Developer menu", Icons.Filled.Info),
                door("gallery", ShellSection.Developer, "Component gallery", Icons.Filled.Star),
                door("playground", ShellSection.Developer, "Playground", Icons.Filled.Create),
            ),
            creations = listOf(
                HomeCreationState("trip", "New trip".toUiText(), Icons.Filled.DateRange, "home_newTripItem"),
                HomeCreationState("item", "New item".toUiText(), Icons.Filled.Build, "home_newItemItem"),
                HomeCreationState("place", "Add a place".toUiText(), Icons.Filled.LocationOn, "home_addPlaceItem"),
            ),
            developerDoors = true,
        )
    }
}

/** A preview door: what a feature's contribution looks like once its figure is in. */
private fun door(
    id: String,
    section: ShellSection,
    title: String,
    icon: ImageVector,
    figure: String? = null,
    badge: Int = 0,
) = HomeDoorState(id, section, title.toUiText(), icon, figure?.toUiText(), badge)
