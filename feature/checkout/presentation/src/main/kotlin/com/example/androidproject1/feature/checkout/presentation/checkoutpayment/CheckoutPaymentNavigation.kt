package com.example.androidproject1.feature.checkout.presentation.checkoutpayment

/** One-off navigation intents, turned into back-stack calls in CheckoutPaymentDestination. */
sealed interface CheckoutPaymentNavigation {

    /** The card is in the draft; the last step is the review. */
    data object Review : CheckoutPaymentNavigation

    /** Back to the delivery, which the draft still holds. */
    data object Back : CheckoutPaymentNavigation
}
