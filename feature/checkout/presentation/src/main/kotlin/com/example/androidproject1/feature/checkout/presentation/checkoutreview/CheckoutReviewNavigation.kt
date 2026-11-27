package com.example.androidproject1.feature.checkout.presentation.checkoutreview

/** One-off navigation intents, turned into back-stack calls in CheckoutReviewDestination. */
sealed interface CheckoutReviewNavigation {

    /** The server took the order: the wizard gives way to it. */
    data class Placed(val orderId: String) : CheckoutReviewNavigation

    /** The bank declined the card: back to the payment step, which still holds it to correct. */
    data object Payment : CheckoutReviewNavigation

    /** Up: back to the payment step. */
    data object Back : CheckoutReviewNavigation
}
