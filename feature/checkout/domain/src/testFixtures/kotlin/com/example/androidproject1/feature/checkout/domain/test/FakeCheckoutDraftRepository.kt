package com.example.androidproject1.feature.checkout.domain.test

import com.example.androidproject1.feature.checkout.domain.CheckoutDraft
import com.example.androidproject1.feature.checkout.domain.CheckoutDraftRepository
import com.example.androidproject1.service.core.domain.result.Outcome

/**
 * In-memory [CheckoutDraftRepository] that keeps what the real one keeps: [saved] is the draft on
 * "disk", its card and payment token already dropped, so a test can rebuild the store from it as
 * a relaunch would.
 */
class FakeCheckoutDraftRepository(var saved: CheckoutDraft? = null) : CheckoutDraftRepository {

    override suspend fun read(): Outcome<CheckoutDraft?> = Outcome.Success(saved)

    override suspend fun save(draft: CheckoutDraft): Outcome<Unit> {
        saved = draft.copy(card = null, paymentToken = null)
        return Outcome.Success(Unit)
    }

    override suspend fun clear(): Outcome<Unit> {
        saved = null
        return Outcome.Success(Unit)
    }
}
