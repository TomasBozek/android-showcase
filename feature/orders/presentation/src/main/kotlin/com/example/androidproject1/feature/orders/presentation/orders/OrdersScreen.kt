package com.example.androidproject1.feature.orders.presentation.orders

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.layout.appStickyHeader
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.presentation.R
import com.example.androidproject1.feature.orders.presentation.component.OrderStatusTag
import com.example.androidproject1.feature.orders.presentation.component.label
import com.example.androidproject1.service.locale.LocalFormats
import java.time.ZoneId

@Composable
fun OrdersScreen(
    state: OrdersState,
    onEvent: (OrdersEvent) -> Unit,
) {
    val formats = LocalFormats.current

    AppScaffold(
        screenId = "OrdersScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.orders_title),
                onNavigateUp = { onEvent(OrdersEvent.UpClicked) },
                navigateUpTestTag = "orders_upButton",
            )
        },
    ) {
        if (state.isEmpty) {
            AppEmptyState(
                icon = Icons.Filled.ShoppingCart,
                title = stringResource(R.string.orders_empty_title),
                message = stringResource(R.string.orders_empty_message),
                actionLabel = stringResource(R.string.orders_empty_action),
                onAction = { onEvent(OrdersEvent.BrowseClicked) },
                actionTestTag = "orders_browseButton",
                modifier = Modifier.testTag("orders_empty"),
            )
            return@AppScaffold
        }
        // Resolved here: a sticky header's title is read inside the list's builder, which is not composable.
        val headers = OrderStatus.entries.associateWith { stringResource(it.label) }
        LazyColumn(modifier = Modifier.fillMaxSize().testTag("orders_list")) {
            state.sections.forEach { section ->
                appStickyHeader(title = headers.getValue(section.status), key = section.status)
                items(items = section.orders, key = { it.id }) { order ->
                    val placedOn = order.placedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                    AppListItem(
                        headline = stringResource(R.string.orders_item, order.id),
                        supporting = stringResource(
                            R.string.orders_item_supporting,
                            formats.date(placedOn),
                            formats.money(order.total),
                        ),
                        onClick = { onEvent(OrdersEvent.OrderClicked(order.id)) },
                        trailing = { OrderStatusTag(status = order.status) },
                        modifier = Modifier.testTag("orders_item_${order.id}"),
                    )
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    OrdersScreen(state = OrdersState.PREVIEW) {}
}

@ScreenPreview
@Composable
private fun EmptyPreview() = ThemedScreenPreview {
    OrdersScreen(state = OrdersState(loaded = true)) {}
}
