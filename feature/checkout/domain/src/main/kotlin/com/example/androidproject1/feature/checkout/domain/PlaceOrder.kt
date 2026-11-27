package com.example.androidproject1.feature.checkout.domain

import com.example.androidproject1.service.core.domain.error.BadRequestError
import com.example.androidproject1.service.core.domain.result.Outcome

/**
 * The one call the review step makes. A use case rather than the bare repository call, so the rule
 * that an order needs an address, a delivery and a card is held once — a draft missing one is
 * refused here, before anything is sent.
 */
class PlaceOrder(private val checkoutRepository: CheckoutRepository) {

    suspend operator fun invoke(draft: CheckoutDraft): Outcome<PlacedOrder> =
        if (draft.isComplete) {
            checkoutRepository.placeOrder(draft)
        } else {
            Outcome.Failure(BadRequestError(message = "an order needs an address, a delivery and a card"))
        }
}
