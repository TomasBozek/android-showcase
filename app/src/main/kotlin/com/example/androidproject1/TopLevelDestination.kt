package com.example.androidproject1

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.cart.api.CartDestination
import com.example.androidproject1.feature.catalog.api.CategoriesDestination
import com.example.androidproject1.feature.home.api.HomeDestination
import com.example.androidproject1.feature.settings.api.SettingsDestination
import com.example.androidproject1.feature.trips.api.TripsDestination

/**
 * A tab of the bottom bar: a top-level destination, and the root of its own back stack.
 *
 * Lives in `:app` for the same reason `AppNavHost` does — it is the one place allowed to know about
 * more than one feature.
 *
 * @property label a string resource in `:app`'s own `strings.xml`, not the feature's: what a screen
 * calls itself and what the tab that leads to it calls itself are not the same decision.
 */
enum class TopLevelDestination(
    val key: NavKey,
    @StringRes val label: Int,
    val icon: ImageVector,
    /**
     * What a flow and a test find this tab by — the project's test-id convention, "find by id, never
     * by text". Carried on the entry rather than written at the call site, so a tab cannot be added
     * without one; `doctor.py` fails on a tab whose tag is not `tabs_<name>Tab`.
     */
    val testTag: String,
) {

    Home(HomeDestination, R.string.tab_home, Icons.Filled.Home, testTag = "tabs_homeTab"),
    Catalog(CategoriesDestination, R.string.tab_catalog, Icons.AutoMirrored.Filled.List, testTag = "tabs_catalogTab"),
    Cart(CartDestination, R.string.tab_cart, Icons.Filled.ShoppingCart, testTag = "tabs_cartTab"),
    Trips(TripsDestination, R.string.tab_trips, Icons.Filled.DateRange, testTag = "tabs_tripsTab"),
    Settings(SettingsDestination, R.string.tab_settings, Icons.Filled.Settings, testTag = "tabs_settingsTab"),
    ;

    companion object {

        fun of(key: NavKey): TopLevelDestination? = entries.firstOrNull { it.key == key }

        /**
         * The tab roots, in the bar's order, for a `TabbedNavigator` — which never names this enum.
         * Home is first, and so is where back from another tab's root lands.
         */
        val roots: List<NavKey> = entries.map { it.key }
    }
}
