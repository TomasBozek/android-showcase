package com.example.androidproject1.feature.catalog.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.choice.AppStepper
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.overlay.AppSheet
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.catalog.presentation.R
import com.example.androidproject1.feature.catalog.presentation.productdetail.ProductDetailEvent
import com.example.androidproject1.service.locale.LocalFormats

/**
 * How many, before a product goes to the cart: a stepper that starts at one and cannot go
 * below it, the line's total beside it, and one confirm. An [AppSheet], so on a tablet it is a
 * centred card; it draws in a window of its own, which is why `OverlayScreenshotTest` in
 * `:core:ui` records one of this shape and no preview can.
 */
@Composable
fun QuantitySheet(
    productName: String,
    priceMinor: Long,
    quantity: Int,
    onEvent: (ProductDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formats = LocalFormats.current
    AppSheet(
        onDismiss = { onEvent(ProductDetailEvent.QuantitySheetDismissed) },
        title = productName,
        modifier = modifier.testTag("productDetail_quantitySheet"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppStepper(
                value = quantity,
                onValueChange = { onEvent(ProductDetailEvent.QuantityChanged(it)) },
                min = 1,
                modifier = Modifier.testTag("productDetail_quantityField"),
            )
            // The line's total, so the count is a price before it is a decision.
            AppText(text = formats.money(priceMinor * quantity), role = TextRole.Numeric)
        }
        AppButton(
            label = stringResource(R.string.product_detail_add_to_cart),
            onClick = { onEvent(ProductDetailEvent.ConfirmAddClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("productDetail_confirmButton"),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // A sheet draws in its own window; this shows the row it would carry.
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
    ) {
        AppStepper(value = 2, onValueChange = {}, min = 1)
        AppText(text = "$9.00", role = TextRole.Numeric)
    }
}
