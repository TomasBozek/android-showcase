package com.example.androidproject1.wiring.checkout

import com.example.androidproject1.feature.checkout.domain.Address
import com.example.androidproject1.feature.checkout.domain.CheckoutPrefill
import com.example.androidproject1.feature.checkout.domain.PaymentCard
import java.time.Year

/**
 * `dev` opens every checkout on an example address and Stripe's published Visa test card, which
 * the fixture engine places rather than declines, so the wizard walks to an order without typing.
 * The expiry is December four years on, so the card never goes stale under the step's own check.
 */
fun checkoutPrefill(): CheckoutPrefill = CheckoutPrefill(
    address = Address(
        name = "Jan Novák",
        street = "Vodičkova 12",
        city = "Praha",
        postalCode = "110 00",
        country = "CZ",
        phone = "+420777123456",
    ),
    card = PaymentCard(
        number = "4242424242424242",
        expiry = "12%02d".format((Year.now().value + CARD_YEARS_LEFT) % CENTURY),
        code = "123",
    ),
)

private const val CARD_YEARS_LEFT = 4
private const val CENTURY = 100
