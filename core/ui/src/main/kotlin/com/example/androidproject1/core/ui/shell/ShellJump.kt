package com.example.androidproject1.core.ui.shell

import androidx.navigation3.runtime.NavKey

/**
 * A screen the debug menu opens in one tap instead of walking the app to it: bound by the
 * feature that owns the screen, so the menu names none and a deleted feature takes its jumps along.
 * A route that takes an argument gets a value the fixtures serve on every flavor, never a blank one.
 *
 * @property id the camelCase stem of the row's test tag: `tripWizard` is `devMenu_tripWizardItem`.
 * @property screenId the screen's own id — what the row shows, and what a flow's `assertVisible` uses.
 */
data class ShellJump(
    val id: String,
    val screenId: String,
    val route: NavKey,
)

/** Every jump the features bind, as one thing a view model takes by constructor, over `getAll()`. */
class ShellJumps(val all: List<ShellJump>)
