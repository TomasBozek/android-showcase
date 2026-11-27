package com.example.androidproject1.feature.checkout.data.source

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:checkout:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface LocalCheckoutDraftDataSource {

    /** The draft as it was stored, or `null` when none is. */
    suspend fun read(): StoredCheckoutDraft?

    /** `null` removes it. */
    suspend fun write(draft: StoredCheckoutDraft?)
}
