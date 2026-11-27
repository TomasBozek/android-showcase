package com.example.androidproject1.feature.checkout.presentation.checkoutpayment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppCardField
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.status.AppStepProgress
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.checkout.presentation.R

@Composable
fun CheckoutPaymentScreen(
    state: CheckoutPaymentState,
    onEvent: (CheckoutPaymentEvent) -> Unit,
) {
    AppScaffold(
        screenId = "CheckoutPaymentScreen",
        // The card form: kept out of screenshots and the recents thumbnail.
        secure = true,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.checkout_payment_title),
                onNavigateUp = { onEvent(CheckoutPaymentEvent.UpClicked) },
                navigateUpTestTag = "checkoutPayment_upButton",
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
                    currentStep = 2,
                    label = stringResource(R.string.checkout_payment_step, 3, CHECKOUT_STEPS),
                    modifier = Modifier.testTag("checkoutPayment_stepProgress"),
                )
                AppCardField(
                    value = state.card,
                    onValueChange = { onEvent(CheckoutPaymentEvent.CardChanged(it)) },
                    onDone = { onEvent(CheckoutPaymentEvent.ContinueClicked) },
                    numberTestTag = "checkoutPayment_cardNumberField",
                    expiryTestTag = "checkoutPayment_expiryField",
                    cvcTestTag = "checkoutPayment_cvcField",
                    modifier = Modifier.fillMaxWidth().testTag("checkoutPayment_cardField"),
                )
                AppText(text = stringResource(R.string.checkout_payment_note), role = TextRole.Secondary)
            }
            if (state.googlePayReady) {
                AppButton(
                    label = stringResource(R.string.checkout_payment_google_pay),
                    onClick = { onEvent(CheckoutPaymentEvent.GooglePayClicked) },
                    kind = ButtonKind.Neutral,
                    modifier = Modifier.fillMaxWidth().testTag("checkoutPayment_googlePayButton"),
                )
            }
            AppButton(
                label = stringResource(R.string.checkout_payment_continue),
                onClick = { onEvent(CheckoutPaymentEvent.ContinueClicked) },
                modifier = Modifier.fillMaxWidth().testTag("checkoutPayment_continueButton"),
            )
        }
    }
}

/** Address, delivery, payment, review. */
private const val CHECKOUT_STEPS = 4

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    CheckoutPaymentScreen(state = CheckoutPaymentState.PREVIEW) {}
}
