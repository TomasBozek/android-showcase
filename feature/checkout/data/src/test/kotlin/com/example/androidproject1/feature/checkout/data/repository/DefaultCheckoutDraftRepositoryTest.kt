package com.example.androidproject1.feature.checkout.data.repository

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.feature.checkout.data.source.DefaultLocalCheckoutDraftDataSource
import com.example.androidproject1.feature.checkout.domain.Address
import com.example.androidproject1.feature.checkout.domain.CheckoutDraft
import com.example.androidproject1.feature.checkout.domain.DeliveryOption
import com.example.androidproject1.feature.checkout.domain.PaymentCard
import com.example.androidproject1.feature.checkout.domain.Promo
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.domain.test.TestDispatchers
import com.example.androidproject1.service.log.domain.test.FakeLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The draft over the real preferences store: what goes in comes out, bar the payment. */
@RunWith(RobolectricTestRunner::class)
class DefaultCheckoutDraftRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val provider = DataStoreProvider(context, name = "checkout_draft_test_${System.nanoTime()}")
    private val repository = DefaultCheckoutDraftRepository(
        logger = FakeLogger(),
        localCheckoutDraftDataSource = DefaultLocalCheckoutDraftDataSource(provider, TestDispatchers),
    )

    @Test
    fun `a saved draft reads back without its card or payment token`() = runTest {
        repository.save(DRAFT.copy(card = CARD, paymentToken = "tok_1"))

        assertEquals(Outcome.Success(DRAFT), repository.read())
    }

    @Test
    fun `the card never reaches the file`() = runTest {
        repository.save(DRAFT.copy(card = CARD))

        val file = provider.dataStore.data.first().asMap().values.joinToString()
        assertFalse(file.contains(CARD.number))
        assertFalse(file.contains(CARD.code))
    }

    @Test
    fun `a cleared draft, or none, reads as none`() = runTest {
        assertEquals(Outcome.Success(null), repository.read())

        repository.save(DRAFT)
        repository.clear()

        assertEquals(Outcome.Success(null), repository.read())
    }

    @Test
    fun `a value this build cannot read is no draft, not a failure`() = runTest {
        provider.dataStore.edit { it[stringPreferencesKey("checkout_draft")] = "{not json" }

        assertEquals(Outcome.Success(null), repository.read())
    }

    private companion object {
        val DRAFT = CheckoutDraft(
            address = Address("Ada Lovelace", "Na Příkopě 1", "Praha", "110 00", "CZ", "+420777123456"),
            delivery = DeliveryOption(id = "courier", name = "Courier", price = 9900, days = 1),
            promo = Promo(code = "WELCOME10", percentOff = 10),
        )
        val CARD = PaymentCard(number = "4242424242424242", expiry = "1229", code = "987")
    }
}
