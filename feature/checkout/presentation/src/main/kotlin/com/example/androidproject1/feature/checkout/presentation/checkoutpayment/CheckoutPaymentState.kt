package com.example.androidproject1.feature.checkout.presentation.checkoutpayment

import androidx.compose.runtime.Immutable
import com.example.androidproject1.core.ui.component.field.AppCardFieldState

/** The card as it is being typed; `AppCardField` words what is wrong with it. */
@Immutable
data class CheckoutPaymentState(
    val card: AppCardFieldState = AppCardFieldState(),
    /** Whether Google Pay can pay on this device now; the button is drawn only then. */
    val googlePayReady: Boolean = false,
) {

    companion object {

        val PREVIEW = CheckoutPaymentState(card = AppCardFieldState.PREVIEW)

        val PREVIEW_GOOGLE_PAY = PREVIEW.copy(googlePayReady = true)
    }
}
