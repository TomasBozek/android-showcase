package com.example.androidproject1.feature.catalog.presentation.productpicker

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface ProductPickerEvent : UiEvent {

    data class ProductClicked(val product: Product) : ProductPickerEvent

    /** The Up arrow was tapped. Every non-root screen has one. */
    data object NavigateUpClicked : ProductPickerEvent

    /** Nothing is cached to pick from; the empty message's action leads to the catalog tab. */
    data object BrowseCatalogClicked : ProductPickerEvent
}

private typealias Product = com.example.androidproject1.feature.catalog.domain.Product
