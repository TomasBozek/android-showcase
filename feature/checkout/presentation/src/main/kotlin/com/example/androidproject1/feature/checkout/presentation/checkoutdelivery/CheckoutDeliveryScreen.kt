package com.example.androidproject1.feature.checkout.presentation.checkoutdelivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.choice.AppRadio
import com.example.androidproject1.core.ui.component.choice.AppRadioGroup
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.status.AppStepProgress
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.checkout.presentation.R
import com.example.androidproject1.service.locale.LocalFormats

@Composable
fun CheckoutDeliveryScreen(
    state: CheckoutDeliveryState,
    onEvent: (CheckoutDeliveryEvent) -> Unit,
) {
    val formats = LocalFormats.current

    AppScaffold(
        screenId = "CheckoutDeliveryScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.checkout_delivery_title),
                onNavigateUp = { onEvent(CheckoutDeliveryEvent.UpClicked) },
                navigateUpTestTag = "checkoutDelivery_upButton",
            )
        },
    ) {
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
                    currentStep = 1,
                    label = stringResource(R.string.checkout_delivery_step, 2, CHECKOUT_STEPS),
                    modifier = Modifier.testTag("checkoutDelivery_stepProgress"),
                )
                AppRadioGroup(modifier = Modifier.testTag("checkoutDelivery_optionList")) {
                    state.options.forEach { option ->
                        val price = if (option.price == 0L) {
                            stringResource(R.string.checkout_delivery_free)
                        } else {
                            formats.money(option.price)
                        }
                        val days = pluralStringResource(R.plurals.checkout_delivery_days, option.days, option.days)
                        AppRadio(
                            selected = option.id == state.selectedId,
                            onSelect = { onEvent(CheckoutDeliveryEvent.OptionSelected(option.id)) },
                            label = stringResource(R.string.checkout_delivery_option, option.name, "$price · $days"),
                            modifier = Modifier.testTag("checkoutDelivery_optionItem_${option.id}"),
                        )
                    }
                }
            }
            AppButton(
                label = stringResource(R.string.checkout_delivery_continue),
                onClick = { onEvent(CheckoutDeliveryEvent.ContinueClicked) },
                modifier = Modifier.fillMaxWidth().testTag("checkoutDelivery_continueButton"),
            )
        }
    }
}

/** Address, delivery, payment, review. */
private const val CHECKOUT_STEPS = 4

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    CheckoutDeliveryScreen(state = CheckoutDeliveryState.PREVIEW) {}
}
