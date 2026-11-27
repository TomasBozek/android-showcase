package com.example.androidproject1.feature.catalog.presentation.categories

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.catalog.domain.Category
import com.example.androidproject1.feature.catalog.domain.Product

@Immutable
data class CategoriesState(
    val categories: List<Category>,
    /** The products kept with the heart on their detail, drawn above the categories; none, no section. */
    val favourites: List<Product> = emptyList(),
    /** A pull-to-refresh is in flight; the indicator, never the overlay, says so. */
    val refreshing: Boolean = false,
    /**
     * The list on screen is what was cached and the server could not be reached. A banner rather
     * than a snackbar: a snackbar is gone in four seconds and the list is stale until a refresh
     * succeeds, which is when this clears.
     */
    val stale: Boolean = false,
) {

    companion object {

        val PREVIEW = CategoriesState(
            categories = listOf(
                Category(id = "beverages", name = "Beverages"),
                Category(id = "bakery", name = "Bakery"),
                Category(id = "produce", name = "Produce"),
            ),
        )

        /** Two favourites above the categories. */
        val PREVIEW_FAVOURITES = PREVIEW.copy(
            favourites = listOf(
                Product(
                    id = "coffee",
                    categoryId = "beverages",
                    name = "Coffee",
                    price = 450,
                    description = "Freshly ground, brewed to order.",
                ),
                Product(
                    id = "croissant",
                    categoryId = "bakery",
                    name = "Croissant",
                    price = 275,
                    description = "Buttery, baked fresh every morning.",
                ),
            ),
        )

        /** Nothing came back — the state the empty message and its Retry are drawn for. */
        val PREVIEW_EMPTY = CategoriesState(categories = emptyList())

        /** A list served from the cache over a dead network. */
        val PREVIEW_STALE = PREVIEW.copy(stale = true)
    }
}
