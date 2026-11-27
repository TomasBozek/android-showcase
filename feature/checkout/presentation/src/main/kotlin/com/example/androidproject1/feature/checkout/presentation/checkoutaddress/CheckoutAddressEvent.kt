package com.example.androidproject1.feature.checkout.presentation.checkoutaddress

import com.example.androidproject1.core.ui.component.field.AppPhoneFieldState
import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface CheckoutAddressEvent : UiEvent {

    data class NameChanged(val name: String) : CheckoutAddressEvent

    data class StreetChanged(val street: String) : CheckoutAddressEvent

    data class CityChanged(val city: String) : CheckoutAddressEvent

    data class PostalCodeChanged(val postalCode: String) : CheckoutAddressEvent

    /** An ISO 3166 region, picked from the list. */
    data class CountryChanged(val country: String) : CheckoutAddressEvent

    data class PhoneChanged(val phone: AppPhoneFieldState) : CheckoutAddressEvent

    data object ContinueClicked : CheckoutAddressEvent

    /** The top bar's up arrow — asks first when there is something to lose, like the gesture. */
    data object UpClicked : CheckoutAddressEvent

    /** The system back gesture on a dirty form; the screen's `DiscardBackHandler` sends it. */
    data object BackRequested : CheckoutAddressEvent
}
