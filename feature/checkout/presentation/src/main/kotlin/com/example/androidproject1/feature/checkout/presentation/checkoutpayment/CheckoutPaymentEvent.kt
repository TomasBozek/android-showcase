package com.example.androidproject1.feature.checkout.presentation.checkoutpayment

import com.example.androidproject1.core.ui.component.field.AppCardFieldState
import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface CheckoutPaymentEvent : UiEvent {

    data class CardChanged(val card: AppCardFieldState) : CheckoutPaymentEvent

    data object ContinueClicked : CheckoutPaymentEvent

    /** The Google Pay button: the sheet, and on a token the review. */
    data object GooglePayClicked : CheckoutPaymentEvent

    data object UpClicked : CheckoutPaymentEvent
}
