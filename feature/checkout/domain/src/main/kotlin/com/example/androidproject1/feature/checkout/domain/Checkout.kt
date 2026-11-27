package com.example.androidproject1.feature.checkout.domain

import com.example.androidproject1.feature.cart.domain.CartItem
import java.time.Instant

/** Where an order goes. [phone] is the international form, `+420777123456`, as a courier dials it. */
data class Address(
    val name: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val phone: String,
)

/**
 * How an order travels, as the server offers it (`GET delivery`).
 *
 * @property price in minor units, like every price here; 0 is free.
 * @property days the working days the server promises, at most.
 */
data class DeliveryOption(
    val id: String,
    val name: String,
    val price: Long,
    val days: Int,
)

/**
 * A card as the payment step holds it: digits only, `expiry` as `MMYY`. It never leaves the data
 * layer as it is — the remote source swaps it for a token first.
 */
data class PaymentCard(
    val number: String,
    val expiry: String,
    val code: String,
)

/** A promo code the server accepted, and what it takes off the lines' subtotal. */
data class Promo(
    val code: String,
    val percentOff: Int,
) {

    /** What it takes off [subtotal], rounded down to a whole minor unit. */
    fun discountOn(subtotal: Long): Long = subtotal * percentOff / PERCENT

    private companion object {

        const val PERCENT = 100
    }
}

/**
 * What the wizard has gathered so far, step by step. Each step fills its own part, and the review
 * step places the order once every part is there ([isComplete]). The lines are not in it: the order
 * is for what is in the cart when it is placed, read then.
 */
data class CheckoutDraft(
    val address: Address? = null,
    val delivery: DeliveryOption? = null,
    val card: PaymentCard? = null,
    val promo: Promo? = null,
    /**
     * What Google Pay made for the payment: a token the server exchanges for the money, in
     * place of a [card]. It wins over a card typed as well — the choice made last is the one that stands.
     */
    val paymentToken: String? = null,
) {

    val isComplete: Boolean get() = address != null && delivery != null && (card != null || paymentToken != null)
}

/**
 * An order the server took. It carries everything the order list and its detail show, so
 * the orders feature records it without asking the server again.
 *
 * @property discount what the promo took off, in minor units; 0 without one.
 * @property total what was charged: the lines, less the discount, plus the delivery.
 */
data class PlacedOrder(
    val id: String,
    val lines: List<CartItem>,
    val address: Address,
    val delivery: DeliveryOption,
    val discount: Long,
    val total: Long,
    val placedAt: Instant,
)
