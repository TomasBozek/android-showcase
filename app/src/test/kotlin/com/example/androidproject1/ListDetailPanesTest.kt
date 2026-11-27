package com.example.androidproject1

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.example.androidproject1.feature.catalog.api.ProductDetailDestination
import com.example.androidproject1.feature.catalog.api.ProductsDestination
import com.example.androidproject1.feature.catalog.presentation.productdetail.productDetailDestination
import com.example.androidproject1.feature.catalog.presentation.products.productsDestination
import com.example.androidproject1.feature.inventory.api.InventoryDestination
import com.example.androidproject1.feature.inventory.api.InventoryDetailDestination
import com.example.androidproject1.feature.inventory.presentation.inventory.inventoryDestination
import com.example.androidproject1.feature.inventory.presentation.inventorydetail.inventoryDetailDestination
import com.example.androidproject1.feature.movies.api.MovieDetailDestination
import com.example.androidproject1.feature.movies.api.MovieFavouritesDestination
import com.example.androidproject1.feature.movies.api.MoviesDestination
import com.example.androidproject1.feature.movies.presentation.moviedetail.movieDetailDestination
import com.example.androidproject1.feature.movies.presentation.moviefavourites.movieFavouritesDestination
import com.example.androidproject1.feature.movies.presentation.movies.moviesDestination
import com.example.androidproject1.feature.orders.api.OrderDetailDestination
import com.example.androidproject1.feature.orders.api.OrdersDestination
import com.example.androidproject1.feature.orders.presentation.orderdetail.orderDetailDestination
import com.example.androidproject1.feature.orders.presentation.orders.ordersDestination
import com.example.androidproject1.feature.trips.api.TripDetailDestination
import com.example.androidproject1.feature.trips.api.TripsDestination
import com.example.androidproject1.feature.trips.api.TripsListDestination
import com.example.androidproject1.feature.trips.presentation.tripdetail.tripDetailDestination
import com.example.androidproject1.feature.trips.presentation.trips.tripsDestination
import com.example.androidproject1.feature.trips.presentation.tripslist.tripsListDestination
import com.example.androidproject1.feature.weather.api.ForecastDestination
import com.example.androidproject1.feature.weather.api.WeatherDestination
import com.example.androidproject1.feature.weather.presentation.forecast.forecastDestination
import com.example.androidproject1.feature.weather.presentation.weather.weatherDestination
import com.example.androidproject1.service.navigation.Navigator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Every list that opens a detail declares itself the list pane and the detail the detail pane, so a
 * wide window shows the two side by side. Read off the entries' metadata: the scene strategy
 * decides the layout from nothing else.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class ListDetailPanesTest {

    private val navigator = object : Navigator {
        override val currentTab: NavKey? = null
        override fun push(route: NavKey) = Unit
        override fun replace(route: NavKey) = Unit
        override fun back() = false
        override fun selectTab(root: NavKey) = false
        override fun setResult(key: String, value: Any?) = Unit
    }

    private val entries = entryProvider<NavKey> {
        productsDestination(navigator)
        productDetailDestination(navigator)
        inventoryDestination(navigator)
        inventoryDetailDestination(navigator)
        ordersDestination(navigator)
        orderDetailDestination(navigator)
        tripsDestination(navigator)
        tripsListDestination(navigator)
        tripDetailDestination(navigator)
        moviesDestination(navigator)
        movieFavouritesDestination(navigator)
        movieDetailDestination(navigator)
        weatherDestination(navigator)
        forecastDestination(navigator)
    }

    private val listPane = ListDetailSceneStrategy.listPane(detailPlaceholder = {}).keys
    private val detailPane = ListDetailSceneStrategy.detailPane().keys

    @Test
    fun `every list that opens a detail is a list pane`() {
        listOf(
            ProductsDestination(categoryId = "coffee"),
            InventoryDestination,
            OrdersDestination,
            TripsDestination,
            TripsListDestination,
            MoviesDestination,
            MovieFavouritesDestination,
            WeatherDestination,
        ).forEach { key -> assertEquals("$key", listPane, entries(key).metadata.keys) }
    }

    @Test
    fun `and every detail it opens is a detail pane`() {
        listOf(
            ProductDetailDestination(productId = "1"),
            InventoryDetailDestination(itemId = "item-1"),
            OrderDetailDestination(orderId = "order-1"),
            TripDetailDestination(tripId = "trip-1"),
            MovieDetailDestination(movieId = 1),
            ForecastDestination(placeId = 1),
        ).forEach { key -> assertEquals("$key", detailPane, entries(key).metadata.keys) }
    }
}
