package com.example.androidproject1.feature.catalog.presentation.categories

import com.example.androidproject1.feature.catalog.domain.Category
import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface CategoriesEvent : UiEvent {

    data class CategoryClicked(val category: Category) : CategoriesEvent

    data object SearchClicked : CategoriesEvent

    /** The remove key on a favourite. The undo lives in the view model. */
    data class FavouriteRemoved(val productId: String) : CategoriesEvent

    /** A pull on the list, or Retry on the empty message: fetch the catalog again. */
    data object RefreshRequested : CategoriesEvent
}
