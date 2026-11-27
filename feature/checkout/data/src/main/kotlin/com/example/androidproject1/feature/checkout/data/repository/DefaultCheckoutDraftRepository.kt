package com.example.androidproject1.feature.checkout.data.repository

import com.example.androidproject1.feature.checkout.data.source.LocalCheckoutDraftDataSource
import com.example.androidproject1.feature.checkout.data.source.StoredAddress
import com.example.androidproject1.feature.checkout.data.source.StoredCheckoutDraft
import com.example.androidproject1.feature.checkout.data.source.StoredDelivery
import com.example.androidproject1.feature.checkout.data.source.StoredPromo
import com.example.androidproject1.feature.checkout.domain.Address
import com.example.androidproject1.feature.checkout.domain.CheckoutDraft
import com.example.androidproject1.feature.checkout.domain.CheckoutDraftRepository
import com.example.androidproject1.feature.checkout.domain.DeliveryOption
import com.example.androidproject1.feature.checkout.domain.Promo
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger

/** The draft to and from its stored shape, which drops the card and the payment token. */
class DefaultCheckoutDraftRepository(
    logger: Logger,
    private val localCheckoutDraftDataSource: LocalCheckoutDraftDataSource,
) : CheckoutDraftRepository, BaseRepository(logger = logger.withTag("DefaultCheckoutDraftRepository")) {

    override suspend fun read(): Outcome<CheckoutDraft?> = execute {
        localCheckoutDraftDataSource.read()?.toDomain()
    }

    override suspend fun save(draft: CheckoutDraft): Outcome<Unit> = execute {
        localCheckoutDraftDataSource.write(draft.toStored())
    }

    override suspend fun clear(): Outcome<Unit> = execute {
        localCheckoutDraftDataSource.write(null)
    }

    private fun StoredCheckoutDraft.toDomain() = CheckoutDraft(
        address = address?.let { Address(it.name, it.street, it.city, it.postalCode, it.country, it.phone) },
        delivery = delivery?.let { DeliveryOption(it.id, it.name, it.price, it.days) },
        promo = promo?.let { Promo(it.code, it.percentOff) },
    )

    private fun CheckoutDraft.toStored() = StoredCheckoutDraft(
        address = address?.let { StoredAddress(it.name, it.street, it.city, it.postalCode, it.country, it.phone) },
        delivery = delivery?.let { StoredDelivery(it.id, it.name, it.price, it.days) },
        promo = promo?.let { StoredPromo(it.code, it.percentOff) },
    )
}
