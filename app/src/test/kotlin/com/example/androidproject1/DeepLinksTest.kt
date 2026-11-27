package com.example.androidproject1

import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.auth.api.ResetPasswordDestination
import com.example.androidproject1.feature.cart.api.CartDestination
import com.example.androidproject1.feature.catalog.api.CategoriesDestination
import com.example.androidproject1.feature.catalog.api.ProductDetailDestination
import com.example.androidproject1.feature.catalog.api.ProductSearchDestination
import com.example.androidproject1.feature.catalog.api.ProductsDestination
import com.example.androidproject1.feature.home.api.HomeDestination
import com.example.androidproject1.feature.inventory.api.InventoryDestination
import com.example.androidproject1.feature.inventory.api.InventoryDetailDestination
import com.example.androidproject1.feature.inventory.api.InventoryEditorDestination
import com.example.androidproject1.feature.movies.api.MovieDetailDestination
import com.example.androidproject1.feature.movies.api.MoviesDestination
import com.example.androidproject1.feature.orders.api.OrderDetailDestination
import com.example.androidproject1.feature.orders.api.OrdersDestination
import com.example.androidproject1.feature.profile.api.ProfileDestination
import com.example.androidproject1.feature.settings.api.SettingsDestination
import com.example.androidproject1.feature.trips.api.TripDetailDestination
import com.example.androidproject1.feature.trips.api.TripWizardDestination
import com.example.androidproject1.feature.trips.api.TripsDestination
import com.example.androidproject1.feature.weather.api.ForecastDestination
import com.example.androidproject1.feature.weather.api.WeatherDestination
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import com.example.androidproject1.service.deeplink.bare
import com.example.androidproject1.service.deeplink.combineDeepLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parser takes a string rather than an `android.net.Uri`, so this is a plain JVM test with
 * nothing to stub. The scheme itself is `MainActivity`'s to check — against `applicationId` —
 * which is why the URIs here carry a neutral one and this file does not name the project.
 *
 * One case per door: the path each feature's table gives, target last.
 */
class DeepLinksTest {

    private fun keys(uri: String): List<NavKey>? = DeepLinks.parse(uri)?.keys

    @Test
    fun `home, catalog, cart, trips and settings are their tabs`() {
        assertEquals(listOf(HomeDestination), keys("app://home"))
        assertEquals(listOf(CategoriesDestination), keys("app://catalog"))
        assertEquals(listOf(CartDestination), keys("app://cart"))
        assertEquals(listOf(TripsDestination), keys("app://trips"))
        assertEquals(listOf(SettingsDestination), keys("app://settings"))
    }

    @Test
    fun `a product link is the product under the catalog`() {
        assertEquals(
            listOf(CategoriesDestination, ProductDetailDestination("croissant")),
            keys("app://product/croissant"),
        )
    }

    @Test
    fun `a category link is the category's products, its name left to the screen`() {
        assertEquals(listOf(CategoriesDestination, ProductsDestination("bakery")), keys("app://category/bakery"))
        assertNull(keys("app://category"))
    }

    @Test
    fun `search is the catalog's search`() {
        assertEquals(listOf(CategoriesDestination, ProductSearchDestination), keys("app://search"))
        assertNull(keys("app://search/coffee"))
    }

    @Test
    fun `a trip link is the trip under the Trips tab`() {
        assertEquals(listOf(TripsDestination, TripDetailDestination("lisbon")), keys("app://trip/lisbon"))
        assertNull(keys("app://trip"))
    }

    @Test
    fun `profile hangs off Settings`() {
        assertEquals(listOf(SettingsDestination, ProfileDestination), keys("app://profile"))
    }

    @Test
    fun `orders are the list under Profile, and order an id under it`() {
        assertEquals(listOf(SettingsDestination, ProfileDestination, OrdersDestination), keys("app://orders"))
        assertEquals(
            listOf(SettingsDestination, ProfileDestination, OrdersDestination, OrderDetailDestination("A10001")),
            keys("app://order/A10001"),
        )
        assertNull(keys("app://order"))
        assertNull(keys("app://orders/A10001"))
    }

    @Test
    fun `inventory is the list, and an id under it an item`() {
        assertEquals(listOf(InventoryDestination), keys("app://inventory"))
        assertEquals(listOf(InventoryDestination, InventoryDetailDestination("drill")), keys("app://inventory/drill"))
    }

    @Test
    fun `movies is the list, and movie an id under it`() {
        assertEquals(listOf(MoviesDestination), keys("app://movies"))
        assertEquals(listOf(MoviesDestination, MovieDetailDestination(550)), keys("app://movie/550"))
    }

    @Test
    fun `weather is the places, and an id under it a forecast`() {
        assertEquals(listOf(WeatherDestination), keys("app://weather"))
        assertEquals(listOf(WeatherDestination, ForecastDestination(3067696)), keys("app://weather/3067696"))
    }

    @Test
    fun `the three create screens have links of their own, for the launcher's shortcuts`() {
        assertEquals(listOf(TripsDestination, TripWizardDestination), keys("app://trips/new"))
        assertEquals(listOf(WeatherDestination), keys("app://weather/search"))

        val item = keys("app://inventory/new")!!
        assertEquals(InventoryDestination, item[0])
        // A new item is a fresh id, minted per link, the way the list mints one.
        val editor = item[1] as InventoryEditorDestination
        assertNotEquals("new", editor.itemId)
        assertNotEquals(editor, keys("app://inventory/new")!![1])
        assertNull(keys("app://trips/old"))
    }

    @Test
    fun `a reset link carries its token and needs no session, and a reset with none is not a link`() {
        val target = DeepLinks.parse("app://reset/fixture")

        assertEquals(listOf(ResetPasswordDestination(token = "fixture")), target?.keys)
        assertFalse(target!!.needsSession)
        assertTrue(DeepLinks.parse("app://product/croissant")!!.needsSession)
        assertNull(keys("app://reset"))
    }

    @Test
    fun `a trailing slash is not a second path segment`() {
        assertEquals(keys("app://product/croissant"), keys("app://product/croissant/"))
    }

    @Test
    fun `a host the app does not serve is not a link`() {
        assertNull(DeepLinks.parse("app://invoice/1"))
    }

    @Test
    fun `a deeper path is not a link, rather than one with the first segment taken`() {
        assertNull(DeepLinks.parse("app://product/croissant/reviews"))
    }

    @Test
    fun `an id that is not a number under a numeric door is not a link`() {
        assertNull(DeepLinks.parse("app://weather/abc"))
        assertNull(DeepLinks.parse("app://movie/abc"))
    }

    @Test
    fun `a door with an id refuses none, and a door with none refuses one`() {
        assertNull(DeepLinks.parse("app://movie"))
        assertNull(DeepLinks.parse("app://product/"))
        assertNull(DeepLinks.parse("app://trips/1"))
        assertNull(DeepLinks.parse("app://profile/ada"))
    }

    @Test
    fun `nothing at all is not a link`() {
        assertNull(DeepLinks.parse(null))
        assertNull(DeepLinks.parse(""))
    }

    @Test(expected = IllegalStateException::class)
    fun `two features claiming one door fail loudly`() {
        combineDeepLinks(mapOf("trips" to bare(TripsDestination)), mapOf("trips" to bare(HomeDestination)))
    }

    @Test
    fun `every table is read once, into one set of doors`() {
        // Parsing any link builds the combined table; a duplicate door would throw here first.
        assertEquals(
            DeepLinkTarget(listOf(CartDestination)),
            DeepLinks.parse("app://cart"),
        )
    }
}
