package com.example.androidproject1.core.ui.shell

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

/**
 * A thing a feature lets the user make from Home's speed dial: bound in the feature's own
 * `di` module beside its door, so Home names no feature and a deleted feature takes its choice
 * with it. Home hides the dial when nothing is bound.
 *
 * @property id the choice's name, unique across the app.
 * @property label a string in the contributing feature's own resources.
 * @property testTag the dial item's id — `home_newTripItem` — so a flow taps it by name.
 * @property order the choice's place in the dial, lowest first.
 * @property route called on each press, so a choice that needs a fresh id mints one every time.
 */
data class ShellCreation(
    val id: String,
    @StringRes val label: Int,
    val icon: ImageVector,
    val testTag: String,
    val order: Int = 0,
    val route: () -> NavKey,
)

/** Every creation the features bind, as one thing a view model takes by constructor, over `getAll()`. */
class ShellCreations(val all: List<ShellCreation>)
