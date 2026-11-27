package com.example.androidproject1.feature.checkout.domain

import com.example.androidproject1.service.core.domain.result.Outcome

/** Implemented in the data layer. Declared here so the domain layer depends on nothing. */
interface CheckoutRepository {

    /** The ways an order can travel, cheapest first as the server lists them. */
    suspend fun deliveryOptions(): Outcome<List<DeliveryOption>>

    /** The promo [code] names, or `Failure(InvalidPromo)`. Case and spaces do not matter. */
    suspend fun applyPromo(code: String): Outcome<Promo>

    /**
     * Places the order for what is in the cart now, and clears the cart once the server took it.
     * A `Failure(PaymentDeclined)` or any other failure leaves the cart as it was.
     */
    suspend fun placeOrder(draft: CheckoutDraft): Outcome<PlacedOrder>
}
