package com.example.androidproject1.feature.checkout.presentation.checkoutreview

import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.cart.domain.CartRepository
import com.example.androidproject1.feature.checkout.domain.CheckoutDraftStore
import com.example.androidproject1.feature.checkout.domain.CheckoutRepository
import com.example.androidproject1.feature.checkout.domain.DeclineReason
import com.example.androidproject1.feature.checkout.domain.InvalidPromo
import com.example.androidproject1.feature.checkout.domain.PaymentDeclined
import com.example.androidproject1.feature.checkout.domain.PlaceOrder
import com.example.androidproject1.feature.checkout.presentation.R
import com.example.androidproject1.service.biometric.BiometricGate
import com.example.androidproject1.service.biometric.GateResult
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.ToastTone
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.launch

/**
 * The wizard's last step. The lines are the cart's, observed, so what is shown is what will be
 * ordered; the address and the delivery are the draft's. Placing goes through [PlaceOrder] behind
 * the overlay: a placed order forgets the draft, says so through the app's outlet — so the toast
 * outlives this screen — and gives way to the order; a declined card is an alert worded from
 * [PaymentDeclined] that returns to the payment step.
 */
class CheckoutReviewViewModel(
    logger: Logger,
    private val cartRepository: CartRepository,
    private val checkoutRepository: CheckoutRepository,
    private val placeOrder: PlaceOrder,
    private val draftStore: CheckoutDraftStore,
    private val biometric: BiometricGate,
) : BaseViewModel<CheckoutReviewState, CheckoutReviewEvent, CheckoutReviewNavigation>(
    initialState = draftStore.draft.value.let { draft ->
        CheckoutReviewState(
            address = draft.address,
            delivery = draft.delivery,
            promoCode = draft.promo?.code.orEmpty(),
            promo = draft.promo,
        )
    },
    logger = logger.withTag("CheckoutReviewViewModel"),
) {

    companion object {

        const val ALERT_ID_DECLINED = "checkout_declined"
    }

    init {
        // What an earlier launch saved — the address, the delivery and the promo; the card
        // never is, and Place says so.
        viewModelScope.launch {
            val draft = draftStore.restore()
            updateData {
                copy(
                    address = address ?: draft.address,
                    delivery = delivery ?: draft.delivery,
                    promo = promo ?: draft.promo,
                    promoCode = promoCode.ifEmpty { draft.promo?.code.orEmpty() },
                )
            }
        }
        observe(
            flow = { cartRepository.observeItems() },
            errorDisplay = ErrorDisplay.Inline,
            onData = { lines -> updateData { copy(lines = lines) } },
        )
    }

    override fun onSystemEvent(event: SystemEvent) {
        val declined = event is SystemEvent.AlertResult && event.id == ALERT_ID_DECLINED
        if (declined) {
            uiState.clearAlert()
            navigate(CheckoutReviewNavigation.Payment)
            return
        }
        super.onSystemEvent(event)
    }

    override fun onUiEvent(event: CheckoutReviewEvent) {
        when (event) {
            is CheckoutReviewEvent.PromoChanged -> updateData { copy(promoCode = event.code, promoError = null) }
            CheckoutReviewEvent.ApplyPromoClicked -> applyPromo()
            CheckoutReviewEvent.PlaceClicked -> stepUpThenPlace()
            CheckoutReviewEvent.UpClicked -> navigate(CheckoutReviewNavigation.Back)
        }
    }

    /** A blank code takes the promo off; an unknown one is the field's error, not a dialog. */
    private fun applyPromo() {
        val code = uiState.value.data?.promoCode?.trim().orEmpty()
        if (code.isEmpty()) {
            viewModelScope.launch { draftStore.update { it.copy(promo = null) } }
            updateData { copy(promo = null, promoError = null) }
            return
        }
        execute(
            loading = overlay(),
            onError = { error ->
                if (error is InvalidPromo) {
                    viewModelScope.launch { draftStore.update { it.copy(promo = null) } }
                    updateData { copy(promo = null, promoError = R.string.checkout_review_promo_invalid.toUiText()) }
                    true
                } else {
                    false
                }
            },
            action = { checkoutRepository.applyPromo(code) },
            onData = { promo ->
                viewModelScope.launch { draftStore.update { it.copy(promo = promo) } }
                updateData { copy(promo = promo, promoCode = promo.code, promoError = null) }
            },
        )
    }

    /**
     * One prompt before the money moves: a biometric or the device credential. A device with
     * no screen lock places at once, as it always has; a refusal leaves the order unplaced.
     */
    private fun stepUpThenPlace() {
        val draft = draftStore.draft.value
        if (draft.card == null && draft.paymentToken == null) {
            // A resumed checkout: the payment is never saved, so it is asked for again.
            showSnackbar(R.string.checkout_review_payment_needed.toUiText())
            navigate(CheckoutReviewNavigation.Payment)
            return
        }
        viewModelScope.launch {
            when (biometric.confirm(R.string.checkout_review_place_prompt.toUiText())) {
                GateResult.Confirmed, GateResult.Unavailable -> place()
                GateResult.Denied -> showToast(R.string.checkout_review_step_up_denied.toUiText(), ToastTone.Error)
                GateResult.Cancelled -> Unit
            }
        }
    }

    private fun place() = execute(
        loading = overlay(R.string.checkout_review_placing.toUiText()),
        onError = { error ->
            if (error is PaymentDeclined) {
                uiState.setAlert(
                    id = ALERT_ID_DECLINED,
                    title = R.string.checkout_review_declined_title.toUiText(),
                    message = when (error.reason) {
                        DeclineReason.InsufficientFunds -> R.string.checkout_review_declined_funds
                        DeclineReason.Declined -> R.string.checkout_review_declined_message
                    }.toUiText(),
                    confirmLabel = R.string.checkout_review_declined_confirm.toUiText(),
                )
                true
            } else {
                false
            }
        },
        action = { placeOrder(draftStore.draft.value) },
        onData = { order ->
            viewModelScope.launch { draftStore.clear() }
            showToast(R.string.checkout_review_placed.toUiText(order.id), ToastTone.Success)
            navigate(CheckoutReviewNavigation.Placed(order.id))
        },
    )
}
