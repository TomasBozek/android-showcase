package com.example.androidproject1.feature.checkout.domain

import com.example.androidproject1.service.core.domain.error.DomainError

/**
 * The bank said no (a `402`). The screen words it from [reason] and sends the user back to the
 * card; the cart is untouched, so nothing is lost by trying another.
 */
class PaymentDeclined(
    val reason: DeclineReason,
    override val cause: Throwable? = null,
) : DomainError(message = "payment declined: $reason")

/** Why a card was declined, as far as a bank tells a shop — which is not very far. */
enum class DeclineReason { Declined, InsufficientFunds }

/** The server has no promo by that code, or no longer honours it. */
class InvalidPromo(
    val code: String,
    override val cause: Throwable? = null,
) : DomainError(message = "invalid promo: $code")
