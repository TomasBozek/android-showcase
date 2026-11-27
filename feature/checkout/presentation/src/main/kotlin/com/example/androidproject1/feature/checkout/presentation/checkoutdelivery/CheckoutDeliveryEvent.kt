package com.example.androidproject1.feature.checkout.presentation.checkoutdelivery

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface CheckoutDeliveryEvent : UiEvent {

    data class OptionSelected(val id: String) : CheckoutDeliveryEvent

    data object ContinueClicked : CheckoutDeliveryEvent

    data object UpClicked : CheckoutDeliveryEvent
}
