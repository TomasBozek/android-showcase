package com.example.androidproject1.feature.checkout.presentation.checkoutreview

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface CheckoutReviewEvent : UiEvent {

    data class PromoChanged(val code: String) : CheckoutReviewEvent

    data object ApplyPromoClicked : CheckoutReviewEvent

    data object PlaceClicked : CheckoutReviewEvent

    data object UpClicked : CheckoutReviewEvent
}
