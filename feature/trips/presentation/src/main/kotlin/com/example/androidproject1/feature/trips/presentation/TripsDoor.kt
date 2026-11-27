package com.example.androidproject1.feature.trips.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import com.example.androidproject1.core.ui.shell.ShellCreation
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.shell.figure
import com.example.androidproject1.feature.trips.api.TripWizardDestination
import com.example.androidproject1.feature.trips.api.TripsDestination
import com.example.androidproject1.feature.trips.api.TripsListDestination
import com.example.androidproject1.feature.trips.domain.TripsRepository
import com.example.androidproject1.service.core.ui.text.toPluralUiText

/** Trips's door: the Trips tab, counting the trips. */
fun tripsDoor(repository: TripsRepository): ShellDoor = ShellDoor(
    id = "trips",
    section = ShellSection.Yours,
    title = R.string.trips_door_title,
    icon = Icons.Filled.DateRange,
    route = TripsDestination,
    placements = setOf(ShellPlacement.Home),
    order = 1,
    figure = repository.observeTrips().figure { R.plurals.trips_door_count.toPluralUiText(it.size, it.size) },
)

/**
 * Trips's choice in Home's speed dial. A new trip: the wizard, from its first step.
 */
fun tripCreation(): ShellCreation = ShellCreation(
    id = "trip",
    label = R.string.trips_create_title,
    icon = Icons.Filled.DateRange,
    testTag = "home_newTripItem",
    order = 0,
) { TripWizardDestination }

/**
 * The screens of trips the debug menu opens in one tap; a route with an argument gets a
 * value the fixtures serve on every flavor.
 */
val tripsJumps: List<ShellJump> = listOf(
    ShellJump(id = "tripsList", screenId = "TripsListScreen", route = TripsListDestination),
    ShellJump(id = "tripWizard", screenId = "TripWizardScreen", route = TripWizardDestination),
)
