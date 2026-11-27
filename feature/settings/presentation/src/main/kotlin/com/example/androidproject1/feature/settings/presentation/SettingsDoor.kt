package com.example.androidproject1.feature.settings.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.shell.figure
import com.example.androidproject1.feature.settings.api.SettingsDestination
import com.example.androidproject1.feature.settings.api.SettingsPermissionsDestination
import com.example.androidproject1.feature.settings.domain.ThemePreference
import com.example.androidproject1.feature.settings.domain.ThemeRepository
import com.example.androidproject1.service.core.ui.text.toUiText

/** Settings' door: the Settings tab, and the theme it is set to. */
fun settingsDoor(themeRepository: ThemeRepository): ShellDoor = ShellDoor(
    id = "settings",
    section = ShellSection.App,
    title = R.string.settings_door_title,
    icon = Icons.Filled.Settings,
    route = SettingsDestination,
    order = 2,
    figure = themeRepository.observeTheme().figure { theme ->
        when (theme) {
            ThemePreference.System -> R.string.settings_door_theme_system
            ThemePreference.Light -> R.string.settings_door_theme_light
            ThemePreference.Dark -> R.string.settings_door_theme_dark
        }.toUiText()
    },
)

/**
 * The screens of settings the debug menu opens in one tap; a route with an argument gets a
 * value the fixtures serve on every flavor.
 */
val settingsJumps: List<ShellJump> = listOf(
    ShellJump(
        id = "settingsPermissions",
        screenId = "SettingsPermissionsScreen",
        route = SettingsPermissionsDestination,
    ),
)
