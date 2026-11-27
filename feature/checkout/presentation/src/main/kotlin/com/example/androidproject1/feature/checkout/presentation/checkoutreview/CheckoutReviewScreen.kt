package com.example.androidproject1.feature.checkout.presentation.checkoutreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.component.list.AppDescriptionList
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.list.DescriptionRow
import com.example.androidproject1.core.ui.component.navigation.AppBottomActionBar
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.status.AppStepProgress
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.checkout.presentation.R
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.locale.LocalFormats

@Composable
fun CheckoutReviewScreen(
    state: CheckoutReviewState,
    onEvent: (CheckoutReviewEvent) -> Unit,
) {
    val formats = LocalFormats.current

    AppScaffold(
        screenId = "CheckoutReviewScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.checkout_review_title),
                onNavigateUp = { onEvent(CheckoutReviewEvent.UpClicked) },
                navigateUpTestTag = "checkoutReview_upButton",
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
            ) {
                AppStepProgress(
                    steps = CHECKOUT_STEPS,
                    currentStep = 3,
                    label = stringResource(R.string.checkout_review_step, 4, CHECKOUT_STEPS),
                    modifier = Modifier.testTag("checkoutReview_stepProgress"),
                )

                AppSectionHeader(title = stringResource(R.string.checkout_review_lines))
                Column(modifier = Modifier.testTag("checkoutReview_itemList")) {
                    state.lines.forEach { line ->
                        AppListItem(
                            headline = line.name,
                            supporting = stringResource(
                                R.string.checkout_review_line,
                                line.quantity,
                                formats.money(line.price),
                            ),
                            trailing = { AppText(text = formats.money(line.lineTotal), role = TextRole.Body) },
                            modifier = Modifier.testTag("checkoutReview_item_${line.productId}"),
                        )
                    }
                }

                AppSectionHeader(title = stringResource(R.string.checkout_review_address))
                state.address?.let { address ->
                    AppText(
                        text = listOf(
                            address.name,
                            address.street,
                            "${address.postalCode} ${address.city}",
                            address.phone,
                        ).joinToString("\n"),
                        role = TextRole.Body,
                        modifier = Modifier.testTag("checkoutReview_addressValue"),
                    )
                }

                AppSectionHeader(title = stringResource(R.string.checkout_review_delivery))
                state.delivery?.let { delivery ->
                    AppText(
                        text = stringResource(
                            R.string.checkout_review_delivery_value,
                            delivery.name,
                            if (delivery.price ==
                                0L
                            ) {
                                stringResource(R.string.checkout_delivery_free)
                            } else {
                                formats.money(delivery.price)
                            },
                        ),
                        role = TextRole.Body,
                        modifier = Modifier.testTag("checkoutReview_deliveryValue"),
                    )
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
                ) {
                    AppTextField(
                        value = state.promoCode,
                        onValueChange = { onEvent(CheckoutReviewEvent.PromoChanged(it)) },
                        label = stringResource(R.string.checkout_review_promo),
                        helperText = state.promo?.let {
                            stringResource(R.string.checkout_review_promo_applied, it.percentOff)
                        },
                        errorText = state.promoError?.resolve(),
                        imeAction = ImeAction.Done,
                        onImeAction = { onEvent(CheckoutReviewEvent.ApplyPromoClicked) },
                        modifier = Modifier.weight(1f).testTag("checkoutReview_promoField"),
                    )
                    AppButton(
                        label = stringResource(R.string.checkout_review_apply),
                        onClick = { onEvent(CheckoutReviewEvent.ApplyPromoClicked) },
                        kind = ButtonKind.Outline,
                        modifier = Modifier.testTag("checkoutReview_applyButton"),
                    )
                }

                AppDescriptionList(
                    rows = listOfNotNull(
                        DescriptionRow(
                            stringResource(R.string.checkout_review_subtotal),
                            formats.money(state.subtotal),
                            numeric = true,
                        ),
                        state.promo?.let {
                            DescriptionRow(
                                stringResource(R.string.checkout_review_discount),
                                "−" + formats.money(state.discount),
                                numeric = true,
                            )
                        },
                        DescriptionRow(
                            stringResource(R.string.checkout_review_delivery),
                            formats.money(state.delivery?.price ?: 0),
                            numeric = true,
                        ),
                        DescriptionRow(
                            stringResource(R.string.checkout_review_total),
                            formats.money(state.total),
                            numeric = true,
                        ),
                    ),
                    modifier = Modifier.testTag("checkoutReview_totalValue"),
                )
            }
            AppBottomActionBar(
                actionLabel = stringResource(R.string.checkout_review_place),
                onAction = { onEvent(CheckoutReviewEvent.PlaceClicked) },
                label = stringResource(R.string.checkout_review_total),
                value = formats.money(state.total),
                actionTestTag = "checkoutReview_placeButton",
            )
        }
    }
}

/** Address, delivery, payment, review. */
private const val CHECKOUT_STEPS = 4

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    CheckoutReviewScreen(state = CheckoutReviewState.PREVIEW) {}
}
