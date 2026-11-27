package com.example.androidproject1.feature.checkout.data.source

import com.example.androidproject1.feature.checkout.domain.Promo

/** The checkout server: delivery options, promo codes and the order itself. */
interface RemoteCheckoutDataSource {

    suspend fun getDeliveryOptions(): List<DeliveryOptionDto>

    /** Throws `InvalidPromo` for a code the server does not know. */
    suspend fun getPromo(code: String): Promo

    /** Throws `PaymentDeclined` for a `402`; any other refusal is the status table's. */
    suspend fun placeOrder(request: PlaceOrderRequest): PlacedOrderDto
}
