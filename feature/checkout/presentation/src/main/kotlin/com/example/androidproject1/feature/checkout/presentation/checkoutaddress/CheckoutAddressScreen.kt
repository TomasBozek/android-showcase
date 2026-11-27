package com.example.androidproject1.feature.checkout.presentation.checkoutaddress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.field.AppFormField
import com.example.androidproject1.core.ui.component.field.AppPhoneField
import com.example.androidproject1.core.ui.component.field.AppSelect
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.component.field.phoneRegions
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.status.AppStepProgress
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.checkout.presentation.R
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.navigation.DiscardBackHandler
import java.util.Locale

@Composable
fun CheckoutAddressScreen(
    state: CheckoutAddressState,
    onEvent: (CheckoutAddressEvent) -> Unit,
) {
    val regions = remember { phoneRegions(Locale.getDefault()) }

    AppScaffold(
        screenId = "CheckoutAddressScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.checkout_address_title),
                onNavigateUp = { onEvent(CheckoutAddressEvent.UpClicked) },
                navigateUpTestTag = "checkoutAddress_upButton",
            )
        },
    ) {
        DiscardBackHandler(dirty = state.isDirty) { onEvent(CheckoutAddressEvent.BackRequested) }

        // The form scrolls; Continue does not, so it is never below the fold on a phone.
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.lg),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
            ) {
                AppStepProgress(
                    steps = CHECKOUT_STEPS,
                    currentStep = 0,
                    label = stringResource(R.string.checkout_address_step, 1, CHECKOUT_STEPS),
                    modifier = Modifier.testTag("checkoutAddress_stepProgress"),
                )
                AppTextField(
                    value = state.name.value,
                    onValueChange = { onEvent(CheckoutAddressEvent.NameChanged(it)) },
                    label = stringResource(R.string.checkout_address_name),
                    errorText = state.name.error?.resolve(),
                    imeAction = ImeAction.Next,
                    contentType = ContentType.PersonFullName,
                    modifier = Modifier.fillMaxWidth().testTag("checkoutAddress_nameField"),
                )
                AppTextField(
                    value = state.street.value,
                    onValueChange = { onEvent(CheckoutAddressEvent.StreetChanged(it)) },
                    label = stringResource(R.string.checkout_address_street),
                    errorText = state.street.error?.resolve(),
                    imeAction = ImeAction.Next,
                    contentType = ContentType.AddressStreet,
                    modifier = Modifier.fillMaxWidth().testTag("checkoutAddress_streetField"),
                )
                AppTextField(
                    value = state.city.value,
                    onValueChange = { onEvent(CheckoutAddressEvent.CityChanged(it)) },
                    label = stringResource(R.string.checkout_address_city),
                    errorText = state.city.error?.resolve(),
                    imeAction = ImeAction.Next,
                    contentType = ContentType.AddressLocality,
                    modifier = Modifier.fillMaxWidth().testTag("checkoutAddress_cityField"),
                )
                AppTextField(
                    value = state.postalCode.value,
                    onValueChange = { onEvent(CheckoutAddressEvent.PostalCodeChanged(it)) },
                    label = stringResource(R.string.checkout_address_postal_code),
                    errorText = state.postalCode.error?.resolve(),
                    imeAction = ImeAction.Next,
                    contentType = ContentType.PostalCode,
                    modifier = Modifier.fillMaxWidth().testTag("checkoutAddress_postalCodeField"),
                )
                AppFormField(label = stringResource(R.string.checkout_address_country)) {
                    AppSelect(
                        options = regions.map { it.name },
                        selectedIndex = regions.indexOfFirst { it.iso == state.country },
                        onSelect = { onEvent(CheckoutAddressEvent.CountryChanged(regions[it].iso)) },
                        modifier = Modifier.testTag("checkoutAddress_countryField"),
                    )
                }
                AppPhoneField(
                    value = state.phone,
                    onValueChange = { onEvent(CheckoutAddressEvent.PhoneChanged(it)) },
                    label = stringResource(R.string.checkout_address_phone),
                    errorText = state.phoneError?.resolve(),
                    imeAction = ImeAction.Done,
                    onImeAction = { onEvent(CheckoutAddressEvent.ContinueClicked) },
                    modifier = Modifier.fillMaxWidth().testTag("checkoutAddress_phoneField"),
                )
            }
            AppButton(
                label = stringResource(R.string.checkout_address_continue),
                onClick = { onEvent(CheckoutAddressEvent.ContinueClicked) },
                modifier = Modifier.fillMaxWidth().testTag("checkoutAddress_continueButton"),
            )
        }
    }
}

/** Address, delivery, payment, review. */
private const val CHECKOUT_STEPS = 4

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    CheckoutAddressScreen(state = CheckoutAddressState.PREVIEW) {}
}

@ScreenPreview
@Composable
private fun EmptyPreview() = ThemedScreenPreview {
    CheckoutAddressScreen(state = CheckoutAddressState(country = "CZ").touchAll()) {}
}
