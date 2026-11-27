package com.example.androidproject1.feature.weather.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import com.example.androidproject1.core.ui.shell.ShellCreation
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.shell.figure
import com.example.androidproject1.feature.weather.api.ForecastDestination
import com.example.androidproject1.feature.weather.api.WeatherDestination
import com.example.androidproject1.feature.weather.domain.WeatherRepository
import com.example.androidproject1.service.core.ui.text.toPluralUiText

/** Weather's door: the saved places. */
fun weatherDoor(repository: WeatherRepository): ShellDoor = ShellDoor(
    id = "weather",
    section = ShellSection.Yours,
    title = R.string.weather_door_title,
    icon = Icons.Filled.LocationOn,
    route = WeatherDestination,
    placements = setOf(ShellPlacement.Home, ShellPlacement.Menu),
    order = 4,
    figure = repository.observeSavedPlaces().figure { R.plurals.weather_door_count.toPluralUiText(it.size, it.size) },
)

/**
 * Weather's choice in Home's speed dial. A place to watch: the weather root, where a
 * place is searched for and saved.
 */
fun placeCreation(): ShellCreation = ShellCreation(
    id = "place",
    label = R.string.weather_create_title,
    icon = Icons.Filled.LocationOn,
    testTag = "home_addPlaceItem",
    order = 2,
) { WeatherDestination }

/**
 * The screens of weather the debug menu opens in one tap; a route with an argument gets a
 * value the fixtures serve on every flavor.
 */
val weatherJumps: List<ShellJump> = listOf(
    ShellJump(id = "weather", screenId = "WeatherScreen", route = WeatherDestination),
    ShellJump(id = "forecast", screenId = "ForecastScreen", route = ForecastDestination(placeId = FIXTURE_PLACE_ID)),
)

/** Praha, the first row of the `dev` flavor's geocoding fixture — saved by the weather root on a tap. */
private const val FIXTURE_PLACE_ID = 3067696
