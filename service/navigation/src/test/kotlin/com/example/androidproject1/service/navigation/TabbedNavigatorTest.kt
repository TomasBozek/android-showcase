package com.example.androidproject1.service.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private data object Home : NavKey
private data object Catalog : NavKey
private data object Cart : NavKey
private data object Trips : NavKey
private data object Settings : NavKey
private data object Login : NavKey
private data object Gallery : NavKey
private data class Product(val id: String) : NavKey

private val tabs = listOf(Home, Catalog, Cart, Trips, Settings)
private val product = Product("coffee")

/**
 * The back-stack arithmetic every tab switch depends on.
 *
 * The tabs do not each hold a list. The stack **is** their concatenation in the order they were
 * last visited, a tab's root is the only thing that starts a segment, and everything after one
 * belongs to it. That is what makes per-tab history survive process death for nothing — it is the
 * list `rememberNavBackStack` already saves — and it is why these are plain tests on a list with
 * nothing to mock.
 */
class TabbedNavigatorTest {

    private fun navigatorOver(vararg keys: NavKey): Pair<NavBackStack<NavKey>, TabbedNavigator> {
        val stack = NavBackStack(*keys)
        return stack to TabbedNavigator(stack, tabs)
    }

    // ---- currentTab -------------------------------------------------------------------------

    @Test
    fun `the auth flow has no tab`() {
        assertNull(navigatorOver(Login).second.currentTab)
    }

    @Test
    fun `an empty stack has no tab`() {
        assertNull(navigatorOver().second.currentTab)
    }

    @Test
    fun `the tab on screen is the last tab root on the stack`() {
        assertEquals(Catalog, navigatorOver(Home, Catalog).second.currentTab)
    }

    @Test
    fun `a screen pushed onto a tab does not change which tab it is`() {
        assertEquals(Catalog, navigatorOver(Catalog, product).second.currentTab)
    }

    @Test
    fun `a screen that is not a tab root never becomes the current tab`() {
        assertEquals(Settings, navigatorOver(Settings, Gallery).second.currentTab)
    }

    // ---- push and replace -------------------------------------------------------------------

    @Test
    fun `push puts the route on top of the tab on screen`() {
        val (stack, navigator) = navigatorOver(Home, Catalog)

        navigator.push(product)

        assertEquals(listOf(Home, Catalog, product), stack.toList())
        assertEquals(Catalog, navigator.currentTab)
    }

    @Test
    fun `pushing the route already on top does not add it twice`() {
        val (stack, navigator) = navigatorOver(Catalog, product)

        navigator.push(Product("coffee"))

        assertEquals(listOf(Catalog, product), stack.toList())
    }

    @Test
    fun `replace swaps the screen on top, so back skips it`() {
        val (stack, navigator) = navigatorOver(Home, Gallery)

        navigator.replace(product)
        navigator.back()

        assertEquals(listOf(Home), stack.toList())
    }

    // ---- selectTab --------------------------------------------------------------------------

    @Test
    fun `an unvisited tab is appended as a new segment`() {
        val (stack, navigator) = navigatorOver(Home)

        navigator.selectTab(Catalog)

        assertEquals(listOf(Home, Catalog), stack.toList())
        assertEquals(Catalog, navigator.currentTab)
    }

    @Test
    fun `re-selecting the current tab pops its segment to the root`() {
        val (stack, navigator) = navigatorOver(Home, Catalog, product)

        // A second tap is "take me to the start of this": the product goes, the other
        // segments stay, and the host is told so the root scrolls to the top.
        assertTrue(navigator.selectTab(Catalog))
        assertEquals(listOf(Home, Catalog), stack.toList())
    }

    @Test
    fun `re-selecting a tab already at its root changes nothing and still says so`() {
        val (stack, navigator) = navigatorOver(Home, Catalog)

        assertTrue(navigator.selectTab(Catalog))
        assertEquals(listOf(Home, Catalog), stack.toList())
    }

    @Test
    fun `selecting another tab is not a re-select`() {
        val (_, navigator) = navigatorOver(Home, Catalog, product)

        assertFalse(navigator.selectTab(Home))
        assertFalse(navigator.selectTab(Trips))
    }

    @Test
    fun `a tab switch keeps each tab's segment`() {
        val (stack, navigator) = navigatorOver(Home, Catalog, product, Cart)

        navigator.selectTab(Catalog)

        assertEquals(listOf(Home, Cart, Catalog, product), stack.toList())
        assertEquals(Catalog, navigator.currentTab)
    }

    @Test
    fun `a segment ends at the next tab root, not at the end of the stack`() {
        val (stack, navigator) = navigatorOver(Home, Gallery, Catalog, product)

        navigator.selectTab(Home)

        assertEquals(listOf(Catalog, product, Home, Gallery), stack.toList())
    }

    @Test
    fun `switching away and back returns to the screen the tab was left on`() {
        val (stack, navigator) = navigatorOver(Catalog, product)

        navigator.selectTab(Cart)
        navigator.selectTab(Catalog)

        assertSame(product, stack.last())
    }

    @Test
    fun `selecting every tab in turn leaves one segment each`() {
        val (stack, navigator) = navigatorOver(Home)

        tabs.forEach(navigator::selectTab)

        assertEquals(tabs, stack.toList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a route that is not a tab root cannot be selected as one`() {
        navigatorOver(Home).second.selectTab(Gallery)
    }

    // ---- back -------------------------------------------------------------------------------

    @Test
    fun `back on a pushed screen pops it`() {
        val (stack, navigator) = navigatorOver(Home, Catalog, product)

        assertTrue(navigator.back())

        assertEquals(listOf(Home, Catalog), stack.toList())
    }

    @Test
    fun `back from a tab root goes home, keeping home's own history`() {
        // Not the tab visited before, but the start, with the screens the user left on it.
        val (stack, navigator) = navigatorOver(Home, Gallery, Catalog)

        assertTrue(navigator.back())

        assertEquals(listOf(Home, Gallery), stack.toList())
        assertEquals(Home, navigator.currentTab)
    }

    @Test
    fun `back from a tab root goes home even when home was never visited`() {
        val (stack, navigator) = navigatorOver(Catalog, Trips)

        assertTrue(navigator.back())

        assertEquals(listOf(Home), stack.toList())
    }

    @Test
    fun `back on home's root answers false and leaves the stack alone`() {
        val (stack, navigator) = navigatorOver(Catalog, Trips, Home)

        assertFalse(navigator.back())

        assertEquals(listOf(Catalog, Trips, Home), stack.toList())
    }

    @Test
    fun `back on an empty stack answers false`() {
        assertFalse(navigatorOver().second.back())
    }

    // ---- results ----------------------------------------------------------------------------

    @Test
    fun `a result set through the navigator is in the store it was handed`() {
        val store = NavResultStore()
        val navigator = TabbedNavigator(NavBackStack(Home), tabs, store)

        navigator.setResult("picked", "coffee")

        assertEquals("coffee", store.consume("picked"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `two tabs sharing a root are refused`() {
        TabbedNavigator(NavBackStack(), listOf(Home, Home))
    }
}
