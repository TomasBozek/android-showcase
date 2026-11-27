package com.example.androidproject1.feature.checkout.domain.test

import com.example.androidproject1.feature.cart.domain.CartItem
import com.example.androidproject1.feature.checkout.domain.Address
import com.example.androidproject1.feature.checkout.domain.CheckoutDraft
import com.example.androidproject1.feature.checkout.domain.CheckoutRepository
import com.example.androidproject1.feature.checkout.domain.DeliveryOption
import com.example.androidproject1.feature.checkout.domain.InvalidPromo
import com.example.androidproject1.feature.checkout.domain.PlacedOrder
import com.example.androidproject1.feature.checkout.domain.Promo
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.result.Outcome
import java.time.Instant

/**
 * In-memory [CheckoutRepository], beside the interface it fakes. [placeWith] is what the next
 * `placeOrder` fails with, if anything; [placed] is every draft it was asked to place.
 */
class FakeCheckoutRepository(
    var options: List<DeliveryOption> = OPTIONS,
    var promos: Map<String, Int> = mapOf("WELCOME10" to 10),
    var lines: List<CartItem> = listOf(CartItem(productId = "coffee", name = "Coffee", price = 450, quantity = 2)),
    var failWith: DomainError? = null,
    var placeWith: DomainError? = null,
) : CheckoutRepository {

    val placed = mutableListOf<CheckoutDraft>()

    override suspend fun deliveryOptions(): Outcome<List<DeliveryOption>> =
        failWith?.let { Outcome.Failure(it) } ?: Outcome.Success(options)

    override suspend fun applyPromo(code: String): Outcome<Promo> {
        failWith?.let { return Outcome.Failure(it) }
        val normalised = code.trim().uppercase()
        return promos[normalised]?.let { Outcome.Success(Promo(normalised, it)) }
            ?: Outcome.Failure(InvalidPromo(normalised))
    }

    override suspend fun placeOrder(draft: CheckoutDraft): Outcome<PlacedOrder> {
        placed += draft
        (placeWith ?: failWith)?.let { return Outcome.Failure(it) }
        val subtotal = lines.sumOf { it.lineTotal }
        val discount = draft.promo?.discountOn(subtotal) ?: 0
        return Outcome.Success(
            PlacedOrder(
                id = "A${placed.size}",
                lines = lines,
                address = draft.address ?: ADDRESS,
                delivery = requireNotNull(draft.delivery),
                discount = discount,
                total = subtotal - discount + requireNotNull(draft.delivery).price,
                placedAt = Instant.parse("2026-09-28T10:00:00Z"),
            ),
        )
    }

    companion object {

        val OPTIONS = listOf(
            DeliveryOption(id = "pickup", name = "Pickup point", price = 0, days = 3),
            DeliveryOption(id = "standard", name = "Standard courier", price = 490, days = 2),
            DeliveryOption(id = "express", name = "Express courier", price = 990, days = 1),
        )

        val ADDRESS = Address("Ada Lovelace", "Na Příkopě 1", "Praha", "110 00", "CZ", "+420777123456")
    }
}
