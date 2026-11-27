package com.example.androidproject1.feature.checkout.domain

import com.example.androidproject1.service.core.domain.result.Outcome

/**
 * Where a checkout's draft is kept between launches: the address, the delivery and the
 * promo. Never the card and never the payment token — what [save] is handed of those is dropped,
 * and [read] returns a draft without them.
 */
interface CheckoutDraftRepository {

    /** The draft saved last, without its payment, or `null` when none is. */
    suspend fun read(): Outcome<CheckoutDraft?>

    suspend fun save(draft: CheckoutDraft): Outcome<Unit>

    suspend fun clear(): Outcome<Unit>
}
