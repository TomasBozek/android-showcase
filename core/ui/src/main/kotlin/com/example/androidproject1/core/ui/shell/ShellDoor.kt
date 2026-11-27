package com.example.androidproject1.core.ui.shell

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.text.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * A place a feature puts in the shell: bound in the feature's own `di` module and read with
 * Koin's `getAll()`, so Home, the drawer and Profile name no feature and a deleted feature takes its
 * door with it.
 *
 * @property id the door's name, unique across the app — `"catalog"`; Home's card is `home_<id>Card`.
 * @property title a string in the contributing feature's own resources.
 * @property route where the door leads. A tab root is selected, anything else pushed — the host's
 *   call, from its tab table.
 * @property placements where the door shows; a door in none is bound and never drawn.
 * @property order the door's place within its [section], lowest first.
 * @property figure the one line under the title, from the feature's own domain; `null` until
 *   it is read, and for a door with nothing to count.
 * @property badge a count worn on the card, and on the tab that has the same route.
 */
data class ShellDoor(
    val id: String,
    val section: ShellSection,
    @StringRes val title: Int,
    val icon: ImageVector,
    val route: NavKey,
    val placements: Set<ShellPlacement> = setOf(ShellPlacement.Home),
    val order: Int = 0,
    val figure: Flow<UiText?> = flowOf(null),
    val badge: Flow<Int> = flowOf(0),
)

/**
 * Every door the features bind, as one thing a view model can take by constructor — so it is
 * registered with `viewModelOf` like every other — bound once in `:core:di` over `getAll()`.
 */
class ShellDoors(val all: List<ShellDoor>)

/** The runs of doors, in the order Home draws them. */
enum class ShellSection { Shop, Yours, App, Developer }

/** Where a door is drawn. */
enum class ShellPlacement { Home, Menu, Profile }

/**
 * A figure from a repository's flow: a success becomes its line, a failure draws nothing new — a
 * count that cannot be read is no reason for a dialog over Home.
 */
fun <T> Flow<Outcome<T>>.figure(line: (T) -> UiText?): Flow<UiText?> =
    mapNotNull { outcome -> (outcome as? Outcome.Success)?.let { line(it.data) } }

/** A badge from a repository's count: a count that cannot be read wears none. */
fun Flow<Outcome<Int>>.badge(): Flow<Int> = map { outcome -> (outcome as? Outcome.Success)?.data ?: 0 }
