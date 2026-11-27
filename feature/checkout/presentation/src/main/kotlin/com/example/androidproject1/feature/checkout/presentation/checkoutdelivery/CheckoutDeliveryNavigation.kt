package com.example.androidproject1.feature.checkout.presentation.checkoutdelivery

/** One-off navigation intents, turned into back-stack calls in CheckoutDeliveryDestination. */
sealed interface CheckoutDeliveryNavigation {

    /** The delivery is in the draft; the next step is the card. */
    data object Payment : CheckoutDeliveryNavigation

    /** Back to the address, which the draft still holds. */
    data object Back : CheckoutDeliveryNavigation
}
