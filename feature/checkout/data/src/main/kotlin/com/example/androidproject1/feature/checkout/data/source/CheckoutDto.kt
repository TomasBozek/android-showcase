package com.example.androidproject1.feature.checkout.data.source

import com.example.androidproject1.feature.checkout.domain.Address
import com.example.androidproject1.feature.checkout.domain.DeclineReason
import com.example.androidproject1.feature.checkout.domain.DeliveryOption
import com.example.androidproject1.feature.checkout.domain.Promo
import kotlinx.serialization.Serializable

/** The wire shape, deliberately not the domain's — a renamed JSON field stops here. */
@Serializable
data class DeliveryOptionDto(
    val id: String,
    val name: String,
    /** Minor units. */
    val price: Long,
    val days: Int,
)

@Serializable
data class PromoDto(
    val code: String,
    val percentOff: Int,
)

/** `POST orders`: the lines by id and count — the server prices them — and a token, never a card. */
@Serializable
data class PlaceOrderRequest(
    val lines: List<OrderLineDto>,
    val address: AddressDto,
    val deliveryId: String,
    val cardToken: String,
    val promoCode: String? = null,
)

@Serializable
data class OrderLineDto(
    val productId: String,
    val quantity: Int,
)

@Serializable
data class AddressDto(
    val name: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val phone: String,
)

/** What `POST orders` answers with a `201`. */
@Serializable
data class PlacedOrderDto(
    val id: String,
    /** Minor units: the lines, less [discount], plus the delivery. */
    val total: Long,
    val discount: Long = 0,
    /** ISO-8601, UTC. */
    val placedAt: String,
)

/** What `POST orders` answers with a `402`. */
@Serializable
data class DeclineDto(
    val reason: String = "",
)

internal fun DeliveryOptionDto.toDomain() = DeliveryOption(id = id, name = name, price = price, days = days)

internal fun PromoDto.toDomain() = Promo(code = code, percentOff = percentOff)

internal fun Address.toDto() = AddressDto(
    name = name,
    street = street,
    city = city,
    postalCode = postalCode,
    country = country,
    phone = phone,
)

internal fun DeclineDto.toDomain(): DeclineReason = when (reason) {
    "insufficient_funds" -> DeclineReason.InsufficientFunds
    else -> DeclineReason.Declined
}
