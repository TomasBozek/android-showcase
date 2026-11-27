package com.example.androidproject1.service.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * The [Navigator] over one flat back stack that holds every tab's history.
 *
 * The tabs do not each hold a list of their own. The back stack **is** their concatenation, in the
 * order the tabs were last visited, and a tab's root is the only thing that starts a segment — so
 * the tab on screen is the last root on the stack, and everything after it is that tab's own
 * history. One flat list is what makes per-tab history survive process death for nothing: it is the
 * list `rememberNavBackStack` already saves, and pushing and popping still work unchanged, because
 * they act on the segment that happens to be last.
 *
 * Holds no state of its own, so building one is free and two over the same stack agree.
 *
 * @param tabs the tab roots, in the bar's order; the first is home, where back from another tab's
 * root lands. Handed in, so this module never names the app's tabs.
 * @param results where [setResult] writes; `null` where nothing reads one.
 */
public class TabbedNavigator(
    private val backStack: NavBackStack<NavKey>,
    private val tabs: List<NavKey>,
    private val results: NavResultStore? = null,
) : Navigator {

    init {
        require(tabs.isNotEmpty()) { "A tabbed navigator needs at least one tab" }
        require(tabs.toSet().size == tabs.size) { "Two tabs share a root, so one would be unreachable" }
    }

    private val home: NavKey get() = tabs.first()

    override val currentTab: NavKey?
        get() = backStack.asReversed().firstOrNull { it in tabs }

    override fun push(route: NavKey) {
        if (backStack.lastOrNull() == route) return
        backStack.add(route)
    }

    override fun replace(route: NavKey) {
        if (backStack.isEmpty()) {
            backStack.add(route)
        } else {
            backStack[backStack.lastIndex] = route
        }
    }

    override fun back(): Boolean {
        val top = backStack.lastOrNull() ?: return false
        if (top !in tabs) {
            backStack.removeAt(backStack.lastIndex)
            return true
        }
        if (top == home) return false

        val homeStart = backStack.indexOf(home)
        val homeSegment = if (homeStart <
            0
        ) {
            listOf(home)
        } else {
            backStack.subList(homeStart, segmentEnd(homeStart)).toList()
        }
        backStack.clear()
        backStack.addAll(homeSegment)
        return true
    }

    override fun selectTab(root: NavKey): Boolean {
        require(root in tabs) { "$root is not a tab root" }
        val start = backStack.indexOf(root)
        if (start < 0) {
            backStack.add(root)
            return false
        }

        val end = segmentEnd(start)
        if (end == backStack.size) {
            backStack.subList(start + 1, end).clear()
            return true
        }

        val segment = backStack.subList(start, end).toList()
        backStack.subList(start, end).clear()
        backStack.addAll(segment)
        return false
    }

    override fun setResult(key: String, value: Any?) {
        checkNotNull(results) { "No NavResultStore was handed to this navigator" }.set(key, value)
    }

    /** The index one past the last key belonging to the segment starting at [start]. */
    private fun segmentEnd(start: Int): Int =
        (start + 1 until backStack.size).firstOrNull { backStack[it] in tabs } ?: backStack.size
}
