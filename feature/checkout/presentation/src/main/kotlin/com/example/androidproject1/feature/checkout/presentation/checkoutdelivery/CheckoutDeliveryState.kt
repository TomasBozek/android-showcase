package com.example.androidproject1.feature.checkout.presentation.checkoutdelivery

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.checkout.domain.DeliveryOption

/** The server's delivery options, and the one picked — `null` while none is. */
@Immutable
data class CheckoutDeliveryState(
    val options: List<DeliveryOption> = emptyList(),
    val selectedId: String? = null,
) {

    val selected: DeliveryOption? get() = options.firstOrNull { it.id == selectedId }

    companion object {

        val PREVIEW = CheckoutDeliveryState(
            options = listOf(
                DeliveryOption(id = "pickup", name = "Pickup point", price = 0, days = 3),
                DeliveryOption(id = "standard", name = "Standard courier", price = 490, days = 2),
                DeliveryOption(id = "express", name = "Express courier", price = 990, days = 1),
            ),
            selectedId = "standard",
        )
    }
}
