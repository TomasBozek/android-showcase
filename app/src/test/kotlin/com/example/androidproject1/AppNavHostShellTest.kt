package com.example.androidproject1

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.shell.ShellCreations
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellDoors
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.domain.Session
import com.example.androidproject1.feature.auth.domain.test.FakeAuthService
import com.example.androidproject1.feature.catalog.api.CategoriesDestination
import com.example.androidproject1.feature.catalog.domain.CatalogRepository
import com.example.androidproject1.feature.catalog.domain.FavouritesRepository
import com.example.androidproject1.feature.catalog.domain.test.FakeCatalogRepository
import com.example.androidproject1.feature.catalog.domain.test.FakeFavouritesRepository
import com.example.androidproject1.feature.catalog.presentation.categories.CategoriesViewModel
import com.example.androidproject1.feature.devmenu.api.DevMenuDestination
import com.example.androidproject1.feature.home.api.HomeDestination
import com.example.androidproject1.feature.home.presentation.home.HomeViewModel
import com.example.androidproject1.feature.inventory.api.InventoryDestination
import com.example.androidproject1.feature.inventory.domain.InventoryRepository
import com.example.androidproject1.feature.inventory.domain.test.FakeInventoryRepository
import com.example.androidproject1.feature.inventory.presentation.inventory.InventoryViewModel
import com.example.androidproject1.feature.profile.api.ProfileDestination
import com.example.androidproject1.feature.profile.domain.ProfileRepository
import com.example.androidproject1.feature.profile.domain.test.FakeProfileRepository
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.telemetry.Analytics
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * That the shell is the design system's, and which of its two shapes a width gets.
 *
 * `AppBottomNav` and `AppNavRail` each have a test in `:core:ui`; what neither can see is the
 * seam in [AppNavHost] — that a tab root on a phone width sits over the bar, on a tablet width
 * beside the rail, and that the five `tabs_*Tab` ids reach the tree either way, which is what every
 * Maestro flow taps. No golden: only the library plugin applies Roborazzi, and the goldens are
 * `:core:ui`'s.
 */
/** Five tabs, the two doors placed in the menu plus the tour row that opens nothing, one Developer. */
private const val MENU_ROWS = 9

@RunWith(RobolectricTestRunner::class)
// The plain Application, not this project's `App`: its `onCreate` starts the real Koin graph,
// and this test wants the small one `AppNavHostAnalyticsTest` composes Home over.
@Config(application = Application::class)
class AppNavHostShellTest {

    // An Android rule over a plain ComponentActivity rather than `createComposeRule()`: the two
    // back cases press the activity's own dispatcher and read `isFinishing` off it.
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // The doors are a test's: the shell names no feature, so what it lists is what is bound.
    private val doors = listOf(
        door("inventory", ShellSection.Yours, InventoryDestination, setOf(ShellPlacement.Home, ShellPlacement.Menu)),
        door("homeOnly", ShellSection.Yours, HomeOnly, setOf(ShellPlacement.Home)),
        door("profile", ShellSection.App, ProfileDestination, setOf(ShellPlacement.Menu)),
        door("devMenu", ShellSection.Developer, DevMenuDestination, setOf(ShellPlacement.Menu)),
    )

    @Suppress("unused")
    private val graph = module {
        single<Analytics> { Analytics.NoOp }
        single<Logger> { FakeLogger() }
        single<CatalogRepository> { FakeCatalogRepository() }
        single<FavouritesRepository> { FakeFavouritesRepository() }
        single<InventoryRepository> { FakeInventoryRepository() }
        single<ProfileRepository> { FakeProfileRepository() }
        // The menu's header reads the session; the door the test walks through is Inventory's.
        single<AuthService> { FakeAuthService().apply { session.value = Session("session-1", "ada@example.com") } }
        single { ShellDoors(doors) }
        single { ShellCreations(emptyList()) }
        viewModelOf(::HomeViewModel)
        viewModelOf(::InventoryViewModel)
        viewModelOf(::CategoriesViewModel)
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun `a compact width draws the bar under the tab root, with every tab in it`() {
        composeHome()

        compose.onNodeWithTag("main_bottomNavGroup").assertIsDisplayed()
        compose.onNodeWithTag("main_navRailGroup").assertDoesNotExist()
        TopLevelDestination.entries.forEach { tab ->
            compose.onNodeWithTag(tab.testTag).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "w840dp-h600dp")
    fun `a wider width draws the rail beside the tab root, with every tab in it`() {
        composeHome()

        compose.onNodeWithTag("main_navRailGroup").assertIsDisplayed()
        compose.onNodeWithTag("main_bottomNavGroup").assertDoesNotExist()
        TopLevelDestination.entries.forEach { tab ->
            compose.onNodeWithTag(tab.testTag).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun `the drawer lists the menu doors`() {
        val backStack = composeHome()

        compose.onNodeWithTag("menu_openButton").performClick()
        compose.waitForIdle()

        // The tabs, then the doors placed in the menu and the tour row, then the Developer run —
        // and not the door placed on Home only.
        compose.onAllNodes(hasTestTagPrefix("menu_") and hasTestTagSuffix("Item")).assertCountEquals(MENU_ROWS)
        listOf("menu_inventoryItem", "menu_profileItem", "menu_tourItem", "menu_devMenuItem").forEach { tag ->
            compose.onNodeWithTag(tag).assertExists()
        }
        compose.onNodeWithTag("menu_homeOnlyItem").assertDoesNotExist()
        compose.onNodeWithTag("menu_inventoryItem").performClick()
        compose.waitForIdle()

        // Closed, not gone: Material keeps the sheet composed off the start edge.
        compose.onNodeWithTag("menu_list").assertIsNotDisplayed()
        assertEquals(InventoryDestination, backStack.last())
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun `back on a tab root goes home`() {
        val backStack = composeHome(CategoriesDestination)
        compose.onNodeWithTag("CategoriesScreen").assertIsDisplayed()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithTag("HomeScreen").assertIsDisplayed()
        assertEquals(listOf<NavKey>(HomeDestination), backStack.toList())
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun `back on Home's root leaves the app`() {
        composeHome()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertEquals(true, compose.activity.isFinishing)
    }

    private fun composeHome(vararg above: NavKey): NavBackStack<NavKey> {
        startKoin { modules(graph) }
        val backStack = NavBackStack<NavKey>(HomeDestination, *above)
        compose.setContent {
            AppTheme {
                // Home rather than the gallery: the gallery is registered only behind
                // `DebugMenu.ENABLED`, so this would not resolve on the `prod` flavor.
                AppNavHost(backStack = backStack)
            }
        }
        compose.waitForIdle()
        return backStack
    }

    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("testTag starts with $prefix") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private fun hasTestTagSuffix(suffix: String) = SemanticsMatcher("testTag ends with $suffix") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.endsWith(suffix) == true
    }

    private fun door(id: String, section: ShellSection, route: NavKey, placements: Set<ShellPlacement>) = ShellDoor(
        id = id,
        section = section,
        title = R.string.tab_home,
        icon = Icons.Filled.Star,
        route = route,
        placements = placements,
    )

    private data object HomeOnly : NavKey
}
