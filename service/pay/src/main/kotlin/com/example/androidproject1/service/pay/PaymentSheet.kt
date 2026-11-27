package com.example.androidproject1.service.pay

import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.result.Outcome

/**
 * Pay with Google Pay: is it ready on this device, and take a payment.
 *
 * **Test environment only** — Google's `ENVIRONMENT_TEST`, which needs no merchant account and
 * charges nothing. What [pay] returns is the *token* Google Pay made for the payment, the thing a
 * gateway would exchange for the money.
 */
public interface PaymentSheet {

    /** Whether the user can pay with it: Google Pay is on the device and a card is saved. */
    public suspend fun isReady(): Boolean

    /**
     * Shows the sheet for [amount] and answers the payment token, or [PaymentCancelledError] when the
     * user dismissed it, or [PaymentUnavailableError] when it could not be shown at all.
     *
     * @param amount in **minor units** — cents, or koruna and heller as the currency has them — the
     * way money is kept everywhere else, so no float ever holds a price.
     * @param currency an ISO 4217 code: `EUR`, `CZK`.
     */
    public suspend fun pay(amount: Long, currency: String): Outcome<String>
}

/** The user closed the sheet without paying. Not a failure to report as one. */
public class PaymentCancelledError : DomainError(message = "The payment sheet was dismissed")

/** Google Pay could not be shown, or answered something this app cannot read. */
public class PaymentUnavailableError(message: String, cause: Throwable? = null) : DomainError(message, cause)
