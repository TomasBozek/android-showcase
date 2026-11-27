package com.example.androidproject1.feature.home.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.androidproject1.core.ui.shell.ShellCreation
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText

/**
 * A door as Home draws it: a [ShellDoor] some feature contributed, its figure and its
 * badge as they last arrived. Beside `component/` rather than in `home/`, because the screen's
 * package holds the screen's six files and nothing else.
 *
 * What a door *does* is not here: the view model looks its route up by [id], and `AppNavHost`
 * decides whether that route is a tab to select or a screen to push.
 *
 * Its card is `home_<id>Card` — the whole card is the press target.
 */
@Immutable
data class HomeDoorState(
    val id: String,
    val section: ShellSection,
    val title: UiText,
    val icon: ImageVector,
    val figure: UiText? = null,
    val badge: Int = 0,
) {
    companion object {

        fun of(door: ShellDoor): HomeDoorState =
            HomeDoorState(id = door.id, section = door.section, title = door.title.toUiText(), icon = door.icon)
    }
}

/** A section's header, Home's own words for the runs the doors are filed under. */
@get:StringRes
val ShellSection.title: Int
    get() = when (this) {
        ShellSection.Shop -> R.string.home_section_shop
        ShellSection.Yours -> R.string.home_section_yours
        ShellSection.App -> R.string.home_section_app
        ShellSection.Developer -> R.string.home_section_developer
    }

/**
 * A choice in Home's speed dial as Home draws it: a [ShellCreation] some feature contributed.
 * Where it leads stays with the view model, which asks the creation for a fresh route on
 * each press.
 */
@Immutable
data class HomeCreationState(
    val id: String,
    val label: UiText,
    val icon: ImageVector,
    val testTag: String,
) {
    companion object {

        fun of(creation: ShellCreation): HomeCreationState =
            HomeCreationState(creation.id, creation.label.toUiText(), creation.icon, creation.testTag)
    }
}
