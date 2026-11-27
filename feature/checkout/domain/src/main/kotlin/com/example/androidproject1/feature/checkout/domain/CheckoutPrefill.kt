package com.example.androidproject1.feature.checkout.domain

/**
 * What every checkout starts from before anything is typed: nothing in a shipped build, and on
 * `dev` an example address and a test card that pass the steps' checks, so the wizard can be
 * walked to a placed order without a keystroke. Bound by `:app`, one per flavor source set, the
 * way `TvMazeConfig` is; [CheckoutDraftStore] opens and clears to it.
 */
data class CheckoutPrefill(
    val address: Address? = null,
    val card: PaymentCard? = null,
) {

    companion object {

        val NONE = CheckoutPrefill()
    }
}
