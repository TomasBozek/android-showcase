package com.example.androidproject1.feature.checkout.presentation.checkoutreview

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.cart.domain.CartItem
import com.example.androidproject1.feature.checkout.domain.Address
import com.example.androidproject1.feature.checkout.domain.DeliveryOption
import com.example.androidproject1.feature.checkout.domain.Promo
import com.example.androidproject1.service.core.ui.text.UiText

/**
 * The last look before the order is placed: the cart's lines as they are now, the draft's address
 * and delivery, and a promo code. The totals are the app's own sum, to show; what is charged is
 * the server's, which the placed order carries.
 */
@Immutable
data class CheckoutReviewState(
    val lines: List<CartItem> = emptyList(),
    val address: Address? = null,
    val delivery: DeliveryOption? = null,
    val promoCode: String = "",
    val promo: Promo? = null,
    val promoError: UiText? = null,
) {

    val subtotal: Long get() = lines.sumOf { it.lineTotal }

    val discount: Long get() = promo?.discountOn(subtotal) ?: 0

    val total: Long get() = subtotal - discount + (delivery?.price ?: 0)

    companion object {

        val PREVIEW = CheckoutReviewState(
            lines = listOf(
                CartItem(productId = "coffee", name = "Coffee", price = 450, quantity = 2),
                CartItem(productId = "tea", name = "Tea", price = 300, quantity = 1),
            ),
            address = Address("Ada Lovelace", "Na Příkopě 1", "Praha", "110 00", "CZ", "+420777123456"),
            delivery = DeliveryOption(id = "standard", name = "Standard courier", price = 490, days = 2),
            promoCode = "WELCOME10",
            promo = Promo("WELCOME10", 10),
        )
    }
}
