package com.example.androidproject1.feature.profile.presentation

import androidx.compose.runtime.Immutable
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText

/**
 * A row under the form: a [ShellDoor] another feature placed on [ShellPlacement.Profile],
 * and its figure as it last arrived. Profile names no feature; its row is `profile_<id>Button`.
 */
@Immutable
data class ProfileDoorState(
    val id: String,
    val title: UiText,
    val figure: UiText? = null,
) {
    companion object {

        fun of(door: ShellDoor): ProfileDoorState = ProfileDoorState(id = door.id, title = door.title.toUiText())
    }
}

/** The doors placed on Profile, in their own order. */
fun List<ShellDoor>.onProfile(): List<ShellDoor> = filter {
    ShellPlacement.Profile in it.placements
}.sortedBy { it.order }
