package com.example.androidproject1

import com.example.androidproject1.feature.auth.api.authDeepLinks
import com.example.androidproject1.feature.cart.api.cartDeepLinks
import com.example.androidproject1.feature.catalog.api.catalogDeepLinks
import com.example.androidproject1.feature.home.api.homeDeepLinks
import com.example.androidproject1.feature.inventory.api.inventoryDeepLinks
import com.example.androidproject1.feature.movies.api.moviesDeepLinks
import com.example.androidproject1.feature.orders.api.ordersDeepLinks
import com.example.androidproject1.feature.profile.api.ProfileDestination
import com.example.androidproject1.feature.profile.api.profileDeepLinks
import com.example.androidproject1.feature.settings.api.SettingsDestination
import com.example.androidproject1.feature.settings.api.settingsDeepLinks
import com.example.androidproject1.feature.trips.api.tripsDeepLinks
import com.example.androidproject1.feature.weather.api.weatherDeepLinks
import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import com.example.androidproject1.service.deeplink.combineDeepLinks

/**
 * The app's own URL scheme, parsed: `<applicationId>://<door>[/<id>]`.
 *
 * Each feature declares its own doors in its api module — the screens it owns, the links to
 * them — and this concatenates the tables, failing at start-up if two claim one door. A plain string
 * in, so this is an ordinary JVM function with no `android.net.Uri` to stub: the scheme itself is
 * checked in `MainActivity`, next to the string resource the manifest's intent filter uses, and by
 * the time a URI reaches here the system has already matched it.
 */
object DeepLinks {

    /**
     * The orders' own doors, declared in orders' api module but reached from Profile under the
     * Settings tab, so the path above them is added here, where both features are in reach.
     */
    private val ordersUnderProfile: Map<String, DeepLinkTable> = ordersDeepLinks.mapValues { (_, table) ->
        DeepLinkTable { segments ->
            table.resolve(segments)?.let {
                it.copy(keys = listOf(SettingsDestination, ProfileDestination) + it.keys)
            }
        }
    }

    private val doors: Map<String, DeepLinkTable> = combineDeepLinks(
        homeDeepLinks,
        catalogDeepLinks,
        cartDeepLinks,
        tripsDeepLinks,
        moviesDeepLinks,
        weatherDeepLinks,
        authDeepLinks,
        settingsDeepLinks,
        profileDeepLinks,
        inventoryDeepLinks,
        ordersUnderProfile,
    )

    fun parse(uri: String?): DeepLinkTarget? {
        val path = uri?.substringAfter(SCHEME_SEPARATOR, missingDelimiterValue = "")
            ?.split("/")
            ?.filter { it.isNotBlank() }
            ?: return null
        if (path.isEmpty()) return null
        return doors[path[0]]?.resolve(path.drop(1))
    }

    private const val SCHEME_SEPARATOR = "://"
}
