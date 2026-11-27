package com.example.androidproject1.feature.checkout.presentation.checkoutaddress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.androidproject1.core.ui.component.field.AppPhoneFieldState
import com.example.androidproject1.feature.checkout.domain.CheckoutDraftStore
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.form.ALERT_ID_DISCARD
import com.example.androidproject1.service.core.ui.form.discardAlert
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.launch

/**
 * The wizard's first step. It opens on the draft's address, untouched: the one typed when the user
 * came back to it, else the flavor's `CheckoutPrefill`. It keeps the form in its own state — so a
 * rotation keeps what was typed — and writes the draft only on Continue. Leaving the step leaves
 * the wizard, so it forgets the draft too, back to the prefill.
 */
class CheckoutAddressViewModel(
    logger: Logger,
    private val draftStore: CheckoutDraftStore,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CheckoutAddressState, CheckoutAddressEvent, CheckoutAddressNavigation>(
    initialState = draftStore.draft.value.address?.let(CheckoutAddressState::forAddress) ?: CheckoutAddressState(),
    logger = logger.withTag("CheckoutAddressViewModel"),
    savedStateHandle = savedStateHandle,
) {

    // What was typed survives process death, as one snapshot of every field written on each edit:
    // `null` until the first edit, and once there it wins over whatever the form opened with.
    private var savedName: String? by saved(KEY_NAME, null)
    private var savedStreet: String? by saved(KEY_STREET, null)
    private var savedCity: String? by saved(KEY_CITY, null)
    private var savedPostalCode: String? by saved(KEY_POSTAL_CODE, null)
    private var savedCountry: String? by saved(KEY_COUNTRY, null)
    private var savedPhoneRegion: String? by saved(KEY_PHONE_REGION, null)
    private var savedPhoneNumber: String? by saved(KEY_PHONE_NUMBER, null)

    init {
        val typed = restore()
        // The draft saved by an earlier launch: the form fills from it unless something
        // typed came back above, which stays and only learns what it is compared against.
        viewModelScope.launch {
            val saved = draftStore.restore().address ?: return@launch
            updateData {
                when {
                    typed -> copy(original = saved)
                    original == null && !isDirty -> CheckoutAddressState.forAddress(saved)
                    else -> this
                }
            }
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_DISCARD) {
            uiState.clearAlert()
            leave()
            return
        }
        super.onSystemEvent(event)
    }

    override fun onUiEvent(event: CheckoutAddressEvent) {
        handle(event)
        if (event.isEdit) remember()
    }

    private fun handle(event: CheckoutAddressEvent) {
        when (event) {
            is CheckoutAddressEvent.NameChanged -> updateData { copy(name = name.changed(event.name)) }
            is CheckoutAddressEvent.StreetChanged -> updateData { copy(street = street.changed(event.street)) }
            is CheckoutAddressEvent.CityChanged -> updateData { copy(city = city.changed(event.city)) }
            is CheckoutAddressEvent.PostalCodeChanged -> updateData {
                copy(postalCode = postalCode.changed(event.postalCode))
            }
            // A phone not yet typed follows the country: it is almost always dialled there.
            is CheckoutAddressEvent.CountryChanged -> updateData {
                copy(
                    country = event.country,
                    phone = if (phone.number.isEmpty()) phone.copy(region = event.country) else phone,
                )
            }
            is CheckoutAddressEvent.PhoneChanged -> updateData { copy(phone = event.phone, phoneTouched = true) }
            CheckoutAddressEvent.ContinueClicked -> next()
            CheckoutAddressEvent.UpClicked ->
                if (uiState.value.data?.isDirty == true) uiState.setAlert(discardAlert()) else leave()
            CheckoutAddressEvent.BackRequested -> uiState.setAlert(discardAlert())
        }
    }

    /**
     * Continue stays enabled on an invalid form: pressing it touches every field, which
     * reveals the errors, and the first is repeated in a snackbar.
     */
    private fun next() {
        val state = uiState.value.data ?: return
        if (!state.canContinue) {
            val revealed = state.touchAll()
            updateData { revealed }
            revealed.firstError?.let { showSnackbar(it) }
            return
        }
        val address = state.toAddress()
        updateData { copy(original = address) }
        viewModelScope.launch {
            draftStore.update { it.copy(address = address) }
            navigate(CheckoutAddressNavigation.Delivery)
        }
    }

    private val CheckoutAddressEvent.isEdit: Boolean
        get() = this is CheckoutAddressEvent.NameChanged ||
            this is CheckoutAddressEvent.StreetChanged ||
            this is CheckoutAddressEvent.CityChanged ||
            this is CheckoutAddressEvent.PostalCodeChanged ||
            this is CheckoutAddressEvent.CountryChanged ||
            this is CheckoutAddressEvent.PhoneChanged

    private fun remember() {
        val state = uiState.value.data ?: return
        savedName = state.name.value
        savedStreet = state.street.value
        savedCity = state.city.value
        savedPostalCode = state.postalCode.value
        savedCountry = state.country
        savedPhoneRegion = state.phone.region
        savedPhoneNumber = state.phone.number
    }

    private fun restore(): Boolean {
        val name = savedName ?: return false
        updateData {
            copy(
                name = this.name.changed(name),
                street = street.changed(savedStreet.orEmpty()),
                city = city.changed(savedCity.orEmpty()),
                postalCode = postalCode.changed(savedPostalCode.orEmpty()),
                country = savedCountry ?: country,
                phone = AppPhoneFieldState(
                    region = savedPhoneRegion ?: phone.region,
                    number = savedPhoneNumber.orEmpty(),
                ),
                phoneTouched = !savedPhoneNumber.isNullOrEmpty(),
            )
        }
        return true
    }

    private fun leave() {
        viewModelScope.launch {
            draftStore.clear()
            navigate(CheckoutAddressNavigation.Back)
        }
    }

    private companion object {

        const val KEY_NAME = "checkout_address_name"
        const val KEY_STREET = "checkout_address_street"
        const val KEY_CITY = "checkout_address_city"
        const val KEY_POSTAL_CODE = "checkout_address_postal_code"
        const val KEY_COUNTRY = "checkout_address_country"
        const val KEY_PHONE_REGION = "checkout_address_phone_region"
        const val KEY_PHONE_NUMBER = "checkout_address_phone_number"
    }
}
