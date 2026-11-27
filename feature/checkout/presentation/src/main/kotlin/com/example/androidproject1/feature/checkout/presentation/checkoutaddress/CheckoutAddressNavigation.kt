package com.example.androidproject1.feature.checkout.presentation.checkoutaddress

/** One-off navigation intents, turned into back-stack calls in CheckoutAddressDestination. */
sealed interface CheckoutAddressNavigation {

    /** The address is in the draft; the next step is the delivery. */
    data object Delivery : CheckoutAddressNavigation

    /** Out of the wizard, back to the cart. The draft is already forgotten. */
    data object Back : CheckoutAddressNavigation
}
