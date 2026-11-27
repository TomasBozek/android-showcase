package com.example.androidproject1.feature.checkout.presentation.checkoutreview

import com.example.androidproject1.feature.cart.domain.CartItem
import com.example.androidproject1.feature.cart.domain.test.FakeCartRepository
import com.example.androidproject1.feature.checkout.domain.CheckoutDraft
import com.example.androidproject1.feature.checkout.domain.CheckoutDraftStore
import com.example.androidproject1.feature.checkout.domain.CheckoutPrefill
import com.example.androidproject1.feature.checkout.domain.DeclineReason
import com.example.androidproject1.feature.checkout.domain.PaymentCard
import com.example.androidproject1.feature.checkout.domain.PaymentDeclined
import com.example.androidproject1.feature.checkout.domain.PlaceOrder
import com.example.androidproject1.feature.checkout.domain.Promo
import com.example.androidproject1.feature.checkout.domain.test.FakeCheckoutDraftRepository
import com.example.androidproject1.feature.checkout.domain.test.FakeCheckoutRepository
import com.example.androidproject1.service.biometric.Availability
import com.example.androidproject1.service.biometric.GateResult
import com.example.androidproject1.service.biometric.test.FakeBiometric
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.test.MainDispatcherRule
import com.example.androidproject1.service.log.domain.test.FakeLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The review: what is ordered, a promo code, and the order placed — or the card declined. */
class CheckoutReviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val coffee = CartItem(productId = "coffee", name = "Coffee", price = 450, quantity = 2)
    private val cart = FakeCartRepository(initial = listOf(coffee))
    private val repository = FakeCheckoutRepository(lines = listOf(coffee))
    private val store = CheckoutDraftStore(CheckoutPrefill.NONE, FakeCheckoutDraftRepository()).apply {
        runBlocking {
            update {
                CheckoutDraft(
                    address = FakeCheckoutRepository.ADDRESS,
                    delivery = FakeCheckoutRepository.OPTIONS[1],
                    card = PaymentCard("4242424242424242", "1230", "123"),
                )
            }
        }
    }

    /** No screen lock unless a test says otherwise, so an order places without asking for one. */
    private val biometric = FakeBiometric(availability = Availability.NoHardware)

    private fun viewModel(draftStore: CheckoutDraftStore = store) = CheckoutReviewViewModel(
        logger = FakeLogger(),
        cartRepository = cart,
        checkoutRepository = repository,
        placeOrder = PlaceOrder(repository),
        draftStore = draftStore,
        biometric = biometric,
    )

    private val CheckoutReviewViewModel.data get() = state.value.data!!

    @Test
    fun `the lines, the address and the delivery are shown, and the total adds up`() = runTest {
        val data = viewModel().data

        assertEquals(listOf(coffee), data.lines)
        assertEquals(FakeCheckoutRepository.ADDRESS, data.address)
        assertEquals(900L, data.subtotal)
        assertEquals(900L + 490L, data.total)
    }

    @Test
    fun `a relaunch shows the saved address, delivery and promo`() = runTest {
        val disk = FakeCheckoutDraftRepository(
            CheckoutDraft(
                address = FakeCheckoutRepository.ADDRESS,
                delivery = FakeCheckoutRepository.OPTIONS[1],
                promo = Promo(code = "WELCOME10", percentOff = 10),
            ),
        )

        val data = viewModel(CheckoutDraftStore(CheckoutPrefill.NONE, disk)).data

        assertEquals(FakeCheckoutRepository.ADDRESS, data.address)
        assertEquals(FakeCheckoutRepository.OPTIONS[1], data.delivery)
        assertEquals("WELCOME10", data.promo?.code)
    }

    @Test
    fun `placing after a relaunch, with no card any more, opens the payment step and places nothing`() = runTest {
        val disk = FakeCheckoutDraftRepository(
            CheckoutDraft(address = FakeCheckoutRepository.ADDRESS, delivery = FakeCheckoutRepository.OPTIONS[1]),
        )
        val viewModel = viewModel(CheckoutDraftStore(CheckoutPrefill.NONE, disk))

        viewModel.onUiEvent(CheckoutReviewEvent.PlaceClicked)

        assertEquals(CheckoutReviewNavigation.Payment, viewModel.navigation.first())
        assertTrue(repository.placed.isEmpty())
    }

    @Test
    fun `a promo code the server knows takes its share off, and is kept in the draft`() = runTest {
        val viewModel = viewModel()

        viewModel.onUiEvent(CheckoutReviewEvent.PromoChanged("welcome10"))
        viewModel.onUiEvent(CheckoutReviewEvent.ApplyPromoClicked)

        assertEquals(Promo("WELCOME10", 10), viewModel.data.promo)
        assertEquals(90L, viewModel.data.discount)
        assertEquals(900L - 90L + 490L, viewModel.data.total)
        assertEquals(Promo("WELCOME10", 10), store.draft.value.promo)
    }

    @Test
    fun `a promo code the server does not know is the field's error`() = runTest {
        val viewModel = viewModel()

        viewModel.onUiEvent(CheckoutReviewEvent.PromoChanged("NOPE"))
        viewModel.onUiEvent(CheckoutReviewEvent.ApplyPromoClicked)

        assertNotNull(viewModel.data.promoError)
        assertNull(viewModel.data.promo)
        assertNull(viewModel.state.value.alert)
    }

    @Test
    fun `placing the order forgets the draft, says so and opens the order`() = runTest {
        val viewModel = viewModel()

        viewModel.onUiEvent(CheckoutReviewEvent.PlaceClicked)

        assertEquals(1, repository.placed.size)
        assertEquals(CheckoutDraft(), store.draft.value)
        assertTrue(viewModel.command.first() is UiCommand.ShowSnackbar)
        assertEquals(CheckoutReviewNavigation.Placed("A1"), viewModel.navigation.first())
    }

    @Test
    fun `with a screen lock, the order is placed after the prompt's yes`() = runTest {
        biometric.availability = Availability.Ready
        val viewModel = viewModel()

        viewModel.onUiEvent(CheckoutReviewEvent.PlaceClicked)

        assertEquals(1, biometric.titles.size)
        assertEquals(1, repository.placed.size)
    }

    @Test
    fun `with a screen lock, a no places nothing and keeps the draft`() = runTest {
        biometric.availability = Availability.Ready
        biometric.answer = GateResult.Denied
        val viewModel = viewModel()

        viewModel.onUiEvent(CheckoutReviewEvent.PlaceClicked)

        assertEquals(0, repository.placed.size)
        assertEquals(FakeCheckoutRepository.ADDRESS, store.draft.value.address)
    }

    @Test
    fun `backing out of the prompt places nothing`() = runTest {
        biometric.availability = Availability.Ready
        biometric.answer = GateResult.Cancelled
        val viewModel = viewModel()

        viewModel.onUiEvent(CheckoutReviewEvent.PlaceClicked)

        assertEquals(0, repository.placed.size)
    }

    @Test
    fun `without a screen lock the order is placed as it always was`() = runTest {
        val viewModel = viewModel()

        viewModel.onUiEvent(CheckoutReviewEvent.PlaceClicked)

        assertEquals(1, repository.placed.size)
    }

    @Test
    fun `a declined card is an alert that says why, and going on returns to the payment`() = runTest {
        repository.placeWith = PaymentDeclined(DeclineReason.InsufficientFunds)
        val viewModel = viewModel()

        viewModel.onUiEvent(CheckoutReviewEvent.PlaceClicked)

        val alert = viewModel.state.value.alert
        assertEquals(CheckoutReviewViewModel.ALERT_ID_DECLINED, alert?.id)
        assertNotNull(store.draft.value.card)

        viewModel.onSystemEvent(SystemEvent.AlertResult.Confirmed(CheckoutReviewViewModel.ALERT_ID_DECLINED))

        assertNull(viewModel.state.value.alert)
        assertEquals(CheckoutReviewNavigation.Payment, viewModel.navigation.first())
    }
}
