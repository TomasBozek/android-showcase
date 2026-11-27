package com.example.androidproject1.feature.devmenu.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The bench, optionally opened on a component.
 *
 * [entryId] is the catalogue id the gallery hands over and is `null` from the menu and from
 * the dev menu's own door, which is the bench's first entry. An id the bench does not have is the
 * same as none: the gallery's catalogue holds seven foundation pages that have no knobs to turn.
 */
@Serializable
data class DevMenuPlaygroundDestination(val entryId: String? = null) : NavKey
