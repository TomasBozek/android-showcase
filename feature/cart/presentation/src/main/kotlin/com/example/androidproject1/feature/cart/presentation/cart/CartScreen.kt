package com.example.androidproject1.feature.cart.presentation.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.choice.AppStepper
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.navigation.AppBottomActionBar
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.layout.TabReselectEffect
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.cart.presentation.R
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.locale.LocalFormats

@Composable
fun CartScreen(
    state: CartState,
    // Hoisted with a default, so the host's tab re-select and a test can reach it.
    listState: LazyListState = rememberLazyListState(),
    onEvent: (CartEvent) -> Unit,
) {
    // A second tap on this tab pops to this root and scrolls it to the top.
    TabReselectEffect { listState.animateScrollToItem(0) }
    AppScaffold(
        screenId = "CartScreen",
        topBar = { AppTopBar(title = stringResource(R.string.cart_title)) },
    ) {
        val formats = LocalFormats.current
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
        ) {
            if (state.isEmpty) {
                AppEmptyState(
                    title = stringResource(R.string.cart_empty_title),
                    message = stringResource(R.string.cart_empty_message),
                    actionLabel = stringResource(R.string.cart_add_item),
                    onAction = { onEvent(CartEvent.AddItemClicked) },
                    modifier = Modifier.testTag("cart_empty"),
                    // The same id as the list's own button: a flow adds the first item the same way.
                    actionTestTag = "cart_addItemButton",
                )
                return@Column
            }

            AppText(text = state.itemCountLabel.resolve(), role = TextRole.Title)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cart_itemList"),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
            ) {
                items(items = state.items, key = { it.productId }) { item ->
                    AppListItem(
                        headline = item.name,
                        supporting = formats.money(item.lineTotal),
                        modifier = appAnimateItem().testTag("cart_item_${item.productId}"),
                        trailing = {
                            AppIconButton(
                                icon = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cart_remove, item.name),
                                onClick = { onEvent(CartEvent.ItemRemoved(item.productId)) },
                                modifier = Modifier.testTag("cart_removeButton"),
                            )
                        },
                    )
                    AppStepper(
                        value = item.quantity,
                        onValueChange = { onEvent(CartEvent.QuantityChanged(item.productId, it)) },
                        min = 0,
                        modifier = Modifier.testTag("cart_quantityField"),
                    )
                }
            }

            AppButton(
                label = stringResource(R.string.cart_add_item),
                onClick = { onEvent(CartEvent.AddItemClicked) },
                kind = ButtonKind.Ghost,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cart_addItemButton"),
            )

            AppBottomActionBar(
                actionLabel = stringResource(R.string.cart_checkout),
                onAction = { onEvent(CartEvent.CheckoutClicked) },
                label = stringResource(R.string.cart_total),
                value = formats.money(state.total),
                actionTestTag = "cart_checkoutButton",
            )
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    CartScreen(state = CartState.PREVIEW) {}
}

/** The empty cart is where the layout collapses, so it is a golden of its own. */
@ScreenPreview
@Composable
private fun EmptyPreview() = ThemedScreenPreview {
    CartScreen(state = CartState.EMPTY) {}
}
