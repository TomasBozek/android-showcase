package com.example.androidproject1.feature.profile.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.shell.figure
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.profile.api.ProfileDestination
import com.example.androidproject1.service.core.ui.text.toUiText

/** Profile's door: who is signed in — the session's email, or a word that nobody is. */
fun profileDoor(authService: AuthService): ShellDoor = ShellDoor(
    id = "profile",
    section = ShellSection.App,
    title = R.string.profile_door_title,
    icon = Icons.Filled.Person,
    route = ProfileDestination,
    placements = setOf(ShellPlacement.Home, ShellPlacement.Menu),
    order = 1,
    figure = authService.observeSession().figure { session ->
        session?.email?.toUiText() ?: R.string.profile_door_empty.toUiText()
    },
)

/**
 * The screens of profile the debug menu opens in one tap; a route with an argument gets a
 * value the fixtures serve on every flavor.
 */
val profileJumps: List<ShellJump> = listOf(
    ShellJump(id = "profile", screenId = "ProfileScreen", route = ProfileDestination),
)
