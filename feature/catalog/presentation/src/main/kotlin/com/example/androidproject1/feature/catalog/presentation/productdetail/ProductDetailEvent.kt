package com.example.androidproject1.feature.catalog.presentation.productdetail

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface ProductDetailEvent : UiEvent {

    /** The heart was tapped. What it means is the ViewModel's business, not the screen's. */
    data object FavouriteToggled : ProductDetailEvent

    /** A link inside the description: opened in a Custom Tab. */
    data class LinkClicked(val url: String) : ProductDetailEvent

    /** Share this product: its name and a link that opens it here. */
    data object ShareClicked : ProductDetailEvent

    /** Add this product to the cart: opens the quantity sheet. */
    data object AddToCartClicked : ProductDetailEvent

    /** The stepper on the sheet. */
    data class QuantityChanged(val quantity: Int) : ProductDetailEvent

    /** The sheet's confirm: the chosen quantity goes to the cart. What "the cart" is stays outside this module. */
    data object ConfirmAddClicked : ProductDetailEvent

    /** The sheet was closed without adding. */
    data object QuantitySheetDismissed : ProductDetailEvent

    /** The Up arrow was tapped. Every non-root screen has one. */
    data object NavigateUpClicked : ProductDetailEvent
}
