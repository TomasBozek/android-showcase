package com.example.androidproject1.feature.checkout.data.source

import kotlinx.serialization.Serializable

/**
 * The draft as it is written to disk. It has no field for a card or a payment token, so
 * neither can reach the file whatever the caller hands over.
 */
@Serializable
data class StoredCheckoutDraft(
    val address: StoredAddress? = null,
    val delivery: StoredDelivery? = null,
    val promo: StoredPromo? = null,
)

@Serializable
data class StoredAddress(
    val name: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val phone: String,
)

@Serializable
data class StoredDelivery(val id: String, val name: String, val price: Long, val days: Int)

@Serializable
data class StoredPromo(val code: String, val percentOff: Int)
