package com.example.androidproject1.feature.checkout.presentation.checkoutaddress

import androidx.compose.runtime.Immutable
import com.example.androidproject1.core.ui.component.field.AppPhoneFieldState
import com.example.androidproject1.core.ui.component.field.defaultPhoneRegion
import com.example.androidproject1.feature.checkout.domain.Address
import com.example.androidproject1.feature.checkout.presentation.R
import com.example.androidproject1.service.core.ui.form.FieldState
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.required
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText

/**
 * The address step's form. [original] is what the step opened with — the draft's address when the
 * user came back to it, else nothing — and what [isDirty] compares against.
 */
@Immutable
data class CheckoutAddressState(
    val name: FieldState = FieldState.of(required()),
    val street: FieldState = FieldState.of(required()),
    val city: FieldState = FieldState.of(required()),
    val postalCode: FieldState = FieldState.of(required()),
    /** ISO 3166, as the address travels. */
    val country: String = defaultPhoneRegion(),
    val phone: AppPhoneFieldState = AppPhoneFieldState(region = country),
    val phoneTouched: Boolean = false,
    val original: Address? = null,
) {

    private val phoneValid: Boolean get() = phone.number.length >= MIN_PHONE_DIGITS

    /** The phone's own rule, shown once the field is touched, the way a [FieldState]'s is. */
    val phoneError: UiText? get() = if (phoneTouched &&
        !phoneValid
    ) {
        R.string.checkout_address_phone_invalid.toUiText()
    } else {
        null
    }

    val canContinue: Boolean get() = Form.canSubmit(name, street, city, postalCode) && phoneValid

    /** The first thing wrong, in the order the fields are drawn — what Continue's snackbar repeats. */
    val firstError: UiText? get() = Form.firstError(name, street, city, postalCode) ?: phoneError

    fun toAddress() = Address(
        name = name.value.trim(),
        street = street.value.trim(),
        city = city.value.trim(),
        postalCode = postalCode.value.trim(),
        country = country,
        phone = phone.international,
    )

    /** Leaving would lose something: anything typed into a blank form, or any change to a kept one. */
    val isDirty: Boolean
        get() = when (original) {
            null -> listOf(name, street, city, postalCode).any { it.value.isNotBlank() } || phone.number.isNotEmpty()
            else -> toAddress() != original
        }

    /** Every field touched, so an untouched form pressed on shows all its errors at once. */
    fun touchAll(): CheckoutAddressState {
        val (touchedName, touchedStreet, touchedCity, touchedPostalCode) = Form.touchAll(name, street, city, postalCode)
        return copy(
            name = touchedName,
            street = touchedStreet,
            city = touchedCity,
            postalCode = touchedPostalCode,
            phoneTouched = true,
        )
    }

    companion object {

        private const val MIN_PHONE_DIGITS = 6

        /** The form filled from [address], untouched — the step revisited. */
        fun forAddress(address: Address): CheckoutAddressState {
            val dialCode = AppPhoneFieldState(region = address.country).dialCode
            val national = if (dialCode.isNotEmpty() && address.phone.startsWith(dialCode)) {
                address.phone.removePrefix(dialCode)
            } else {
                address.phone.filter(Char::isDigit)
            }
            return CheckoutAddressState(
                name = FieldState.of(required(), value = address.name),
                street = FieldState.of(required(), value = address.street),
                city = FieldState.of(required(), value = address.city),
                postalCode = FieldState.of(required(), value = address.postalCode),
                country = address.country,
                phone = AppPhoneFieldState(region = address.country, number = national),
                original = address,
            )
        }

        val PREVIEW = forAddress(Address("Ada Lovelace", "Na Příkopě 1", "Praha", "110 00", "CZ", "+420777123456"))
    }
}
