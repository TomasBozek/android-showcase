package com.example.androidproject1.feature.orders.presentation.orderdetail

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
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppDescriptionList
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.list.DescriptionRow
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.status.AppStepProgress
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.presentation.R
import com.example.androidproject1.feature.orders.presentation.component.OrderStatusTag
import com.example.androidproject1.feature.orders.presentation.component.label
import com.example.androidproject1.service.locale.LocalFormats
import java.time.ZoneId

@Composable
fun OrderDetailScreen(
    state: OrderDetailState,
    onEvent: (OrderDetailEvent) -> Unit,
) {
    val formats = LocalFormats.current

    AppScaffold(
        screenId = "OrderDetailScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.order_detail_title, state.orderId),
                onNavigateUp = { onEvent(OrderDetailEvent.UpClicked) },
                navigateUpTestTag = "orderDetail_upButton",
            )
        },
    ) {
        val order = state.order ?: return@AppScaffold
        val zone = ZoneId.systemDefault()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
            ) {
                AppText(
                    text = stringResource(
                        R.string.order_detail_placed_on,
                        formats.date(order.placedAt.atZone(zone).toLocalDate()),
                    ),
                    role = TextRole.Secondary,
                    modifier = Modifier.weight(1f),
                )
                OrderStatusTag(status = order.status)
            }

            // The way an order moves, as steps; a cancelled one stops where it was stopped.
            val reached = TIMELINE.indexOfLast { step -> order.history.any { it.status == step } }.coerceAtLeast(0)
            AppStepProgress(
                steps = TIMELINE.size,
                currentStep = reached,
                label = stringResource(order.status.label),
                modifier = Modifier.testTag("orderDetail_timelineProgress"),
            )
            AppDescriptionList(
                rows = order.history.reversed().map { change ->
                    val at = change.at.atZone(zone)
                    DescriptionRow(
                        label = stringResource(change.status.label),
                        value = "${formats.date(at.toLocalDate())} ${formats.time(at.toLocalTime())}",
                    )
                },
                modifier = Modifier.testTag("orderDetail_historyList"),
            )

            AppSectionHeader(title = stringResource(R.string.order_detail_lines))
            Column {
                order.lines.forEach { line ->
                    AppListItem(
                        headline = line.name,
                        supporting = stringResource(
                            R.string.order_detail_line,
                            line.quantity,
                            formats.money(line.price),
                        ),
                        trailing = { AppText(text = formats.money(line.lineTotal), role = TextRole.Body) },
                        modifier = Modifier.testTag("orderDetail_item_${line.productId}"),
                    )
                }
            }

            AppSectionHeader(title = stringResource(R.string.order_detail_address))
            AppText(
                text = with(order.address) { listOf(name, street, "$postalCode $city", phone).joinToString("\n") },
                role = TextRole.Body,
                modifier = Modifier.testTag("orderDetail_addressValue"),
            )

            AppDescriptionList(
                rows = listOfNotNull(
                    DescriptionRow(order.deliveryName, formats.money(order.deliveryPrice), numeric = true),
                    DescriptionRow(
                        stringResource(R.string.order_detail_discount),
                        "−" + formats.money(order.discount),
                        numeric = true,
                    )
                        .takeIf { order.discount > 0 },
                    DescriptionRow(
                        stringResource(R.string.order_detail_total),
                        formats.money(order.total),
                        numeric = true,
                    ),
                ),
                modifier = Modifier.fillMaxWidth().testTag("orderDetail_totalValue"),
            )
        }
    }
}

/** The statuses a delivered order passes through, in order; cancelled is not a step. */
private val TIMELINE = listOf(OrderStatus.Placed, OrderStatus.Packed, OrderStatus.Shipped, OrderStatus.Delivered)

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    OrderDetailScreen(state = OrderDetailState.PREVIEW) {}
}
