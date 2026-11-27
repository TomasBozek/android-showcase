package com.example.androidproject1.service.navigation

import androidx.navigation3.runtime.NavKey

/**
 * How a destination moves the user: it names a route, and the rules of the back stack —
 * a segment per tab, reselect to the root, back from a tab root goes home — are this interface's,
 * not every call site's. Never the raw `NavBackStack`.
 */
public interface Navigator {

    /** The root of the tab on screen, or `null` outside the tabbed flow. */
    public val currentTab: NavKey?

    /** Puts [route] on top of the tab on screen. A [route] already on top is not added twice. */
    public fun push(route: NavKey)

    /** Swaps the screen on top for [route], so back skips the one it replaced. */
    public fun replace(route: NavKey)

    /**
     * What back does: a pushed screen pops, a tab root that is not home goes home.
     *
     * @return whether the stack changed; `false` means the user is at the start and leaves.
     */
    public fun back(): Boolean

    /**
     * Brings the tab whose root is [root] to the front, keeping the screens the user left on it; the
     * tab already on screen is popped to its root.
     *
     * @return whether [root] was already the tab on screen — a re-select, for the host to signal.
     */
    public fun selectTab(root: NavKey): Boolean

    /** The responder's half of a result (see [NavResultEffect]): set it under [key], then [back]. */
    public fun setResult(key: String, value: Any?)
}
