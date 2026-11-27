package com.example.androidproject1.feature.checkout.presentation.checkoutdelivery

import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.checkout.domain.CheckoutDraftStore
import com.example.androidproject1.feature.checkout.domain.CheckoutRepository
import com.example.androidproject1.feature.checkout.presentation.R
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.launch

/**
 * The wizard's second step. The options are the server's, asked for once, behind the overlay; the
 * one the draft already holds is picked, else the first — the server lists them cheapest first.
 */
class CheckoutDeliveryViewModel(
    logger: Logger,
    private val checkoutRepository: CheckoutRepository,
    private val draftStore: CheckoutDraftStore,
) : BaseViewModel<CheckoutDeliveryState, CheckoutDeliveryEvent, CheckoutDeliveryNavigation>(
    initialState = CheckoutDeliveryState(selectedId = draftStore.draft.value.delivery?.id),
    logger = logger.withTag("CheckoutDeliveryViewModel"),
) {

    init {
        // The saved draft first, so the list lands on the delivery chosen before a relaunch.
        viewModelScope.launch {
            val saved = draftStore.restore().delivery?.id
            if (saved != null) updateData { copy(selectedId = selectedId ?: saved) }
            load()
        }
    }

    override fun onUiEvent(event: CheckoutDeliveryEvent) {
        when (event) {
            is CheckoutDeliveryEvent.OptionSelected -> updateData { copy(selectedId = event.id) }
            CheckoutDeliveryEvent.ContinueClicked -> next()
            CheckoutDeliveryEvent.UpClicked -> navigate(CheckoutDeliveryNavigation.Back)
        }
    }

    // Inline: the list is what this screen is, so a failure takes its place with a Retry.
    private fun load() = execute(
        loading = overlay(),
        errorDisplay = ErrorDisplay.Inline,
        action = { checkoutRepository.deliveryOptions() },
        onData = { options ->
            updateData {
                copy(
                    options = options,
                    selectedId =
                    selectedId?.takeIf { id -> options.any { it.id == id } } ?: options.firstOrNull()?.id,
                )
            }
        },
    )

    private fun next() {
        val option = uiState.value.data?.selected
        if (option == null) {
            showSnackbar(R.string.checkout_delivery_choose.toUiText())
            return
        }
        viewModelScope.launch {
            draftStore.update { it.copy(delivery = option) }
            navigate(CheckoutDeliveryNavigation.Payment)
        }
    }
}
