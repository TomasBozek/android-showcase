package com.example.androidproject1.feature.checkout.domain

import java.util.Currency
import java.util.Locale

/**
 * The ISO 4217 code a price is charged in: the device's own currency, the one `Formats.money` writes
 * it in, and koruna where the locale names none.
 */
object CheckoutCurrency {

    private const val FALLBACK = "CZK"

    fun code(locale: Locale = Locale.getDefault()): String =
        runCatching { Currency.getInstance(locale).currencyCode }.getOrDefault(FALLBACK)
}
