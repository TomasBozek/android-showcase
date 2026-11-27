package com.example.androidproject1.feature.checkout.presentation.checkoutpayment

import androidx.lifecycle.viewModelScope
import com.example.androidproject1.core.ui.component.field.AppCardFieldState
import com.example.androidproject1.feature.cart.domain.CartRepository
import com.example.androidproject1.feature.checkout.domain.CheckoutCurrency
import com.example.androidproject1.feature.checkout.domain.CheckoutDraftStore
import com.example.androidproject1.feature.checkout.domain.PaymentCard
import com.example.androidproject1.feature.checkout.presentation.R
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.pay.PaymentCancelledError
import com.example.androidproject1.service.pay.PaymentSheet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The wizard's third step. It opens on the draft's card, which is how a declined card comes back
 * to be corrected and how `dev`'s prefilled test card arrives, and writes the draft only on Continue — which stays enabled: a card not
 * typed in full, or one the field has an error for, is said in a snackbar.
 *
 * **Google Pay is the other way through**: its button is there only while the device can pay
 * with it ([PaymentSheet.isReady]), and a payment leaves its token in the draft in place of a card
 * and goes straight to the review. Dismissing the sheet is not an error: the step stays as it was.
 */
class CheckoutPaymentViewModel(
    logger: Logger,
    private val draftStore: CheckoutDraftStore,
    private val paymentSheet: PaymentSheet,
    private val cartRepository: CartRepository,
) : BaseViewModel<CheckoutPaymentState, CheckoutPaymentEvent, CheckoutPaymentNavigation>(
    initialState = CheckoutPaymentState(
        card =
        draftStore.draft.value.card?.let {
            AppCardFieldState(it.number, it.expiry, it.code)
        } ?: AppCardFieldState(),
    ),
    logger = logger.withTag("CheckoutPaymentViewModel"),
) {

    init {
        // Asked once when the step opens: a device that cannot pay with it never shows the button.
        viewModelScope.launch {
            val ready = paymentSheet.isReady()
            updateData { copy(googlePayReady = ready) }
        }
    }

    override fun onUiEvent(event: CheckoutPaymentEvent) {
        when (event) {
            is CheckoutPaymentEvent.CardChanged -> updateData { copy(card = event.card) }
            CheckoutPaymentEvent.ContinueClicked -> next()
            CheckoutPaymentEvent.GooglePayClicked -> payWithGooglePay()
            CheckoutPaymentEvent.UpClicked -> navigate(CheckoutPaymentNavigation.Back)
        }
    }

    private fun next() {
        val card = uiState.value.data?.card ?: return
        if (!card.isComplete()) {
            showSnackbar(R.string.checkout_payment_incomplete.toUiText())
            return
        }
        // A card typed after a Google Pay payment is the choice that stands: the token goes.
        viewModelScope.launch {
            draftStore.update {
                it.copy(
                    card = PaymentCard(number = card.number, expiry = card.expiry, code = card.code),
                    paymentToken = null,
                )
            }
            navigate(CheckoutPaymentNavigation.Review)
        }
    }

    /** What the sheet is asked to charge: the cart's lines, less the promo, plus the delivery — the review's own sum. */
    private fun payWithGooglePay() = execute(
        onError = { it is PaymentCancelledError },
        action = {
            when (val items = cartRepository.observeItems().first()) {
                is Outcome.Failure -> Outcome.Failure(items.error)
                is Outcome.Success -> {
                    val draft = draftStore.restore()
                    val subtotal = items.data.sumOf { it.lineTotal }
                    val amount = subtotal - (draft.promo?.discountOn(subtotal) ?: 0) + (draft.delivery?.price ?: 0)
                    paymentSheet.pay(amount, CheckoutCurrency.code())
                }
            }
        },
        onData = { token ->
            viewModelScope.launch {
                draftStore.update { it.copy(paymentToken = token) }
                navigate(CheckoutPaymentNavigation.Review)
            }
        },
    )
}
