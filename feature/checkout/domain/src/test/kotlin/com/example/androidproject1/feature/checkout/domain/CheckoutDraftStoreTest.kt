package com.example.androidproject1.feature.checkout.domain

import com.example.androidproject1.feature.checkout.domain.test.FakeCheckoutDraftRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The draft between the wizard's steps, and what of it outlives the app. */
class CheckoutDraftStoreTest {

    private val disk = FakeCheckoutDraftRepository()

    private fun store(prefill: CheckoutPrefill = CheckoutPrefill.NONE) = CheckoutDraftStore(prefill, disk)

    @Test
    fun `a relaunch finds the address, the delivery and the promo, and never the card`() = runTest {
        store().apply {
            restore()
            update { it.copy(address = ADDRESS, delivery = DELIVERY, card = CARD, promo = PROMO) }
        }

        val relaunched = store().restore()

        assertEquals(ADDRESS, relaunched.address)
        assertEquals(DELIVERY, relaunched.delivery)
        assertEquals(PROMO, relaunched.promo)
        assertNull(relaunched.card)
    }

    @Test
    fun `a payment token is never kept either`() = runTest {
        store().apply {
            restore()
            update { it.copy(address = ADDRESS, paymentToken = "tok_1") }
        }

        assertNull(store().restore().paymentToken)
    }

    @Test
    fun `a saved address wins over the prefill, and a card still comes from it`() = runTest {
        disk.saved = CheckoutDraft(address = ADDRESS)

        val draft = store(CheckoutPrefill(address = OTHER_ADDRESS, card = CARD)).restore()

        assertEquals(ADDRESS, draft.address)
        assertEquals(CARD, draft.card)
    }

    @Test
    fun `an edit made before the saved draft was read does not wipe it`() = runTest {
        disk.saved = CheckoutDraft(address = ADDRESS)

        store().update { it.copy(delivery = DELIVERY) }

        assertEquals(CheckoutDraft(address = ADDRESS, delivery = DELIVERY), disk.saved)
    }

    @Test
    fun `clearing starts the next checkout from nothing, after a relaunch too`() = runTest {
        store().apply {
            restore()
            update { it.copy(address = ADDRESS, delivery = DELIVERY) }
            clear()
        }

        assertNull(disk.saved)
        assertEquals(CheckoutDraft(), store().restore())
    }

    @Test
    fun `a sign-out erases the draft, in memory and on disk`() = runTest {
        val store = store().apply {
            restore()
            update { it.copy(address = ADDRESS) }
        }

        store.erase()

        assertNull(disk.saved)
        assertEquals(CheckoutDraft(), store.draft.value)
    }

    private companion object {
        val ADDRESS = Address("Ada Lovelace", "Na Příkopě 1", "Praha", "110 00", "CZ", "+420777123456")
        val OTHER_ADDRESS = ADDRESS.copy(name = "Example")
        val DELIVERY = DeliveryOption(id = "courier", name = "Courier", price = 9900, days = 1)
        val CARD = PaymentCard(number = "4242424242424242", expiry = "1229", code = "123")
        val PROMO = Promo(code = "WELCOME10", percentOff = 10)
    }
}
