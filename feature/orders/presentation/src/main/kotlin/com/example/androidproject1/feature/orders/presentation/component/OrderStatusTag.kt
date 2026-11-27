package com.example.androidproject1.feature.orders.presentation.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.status.AppTag
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.presentation.R

/**
 * An order's status as a tag: grey while it is being prepared, blue on its way, green once it has
 * arrived and red if it was cancelled — the one place a status is given a word and a tone, so the
 * list and the detail cannot disagree.
 */
@Composable
fun OrderStatusTag(
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    AppTag(label = stringResource(status.label), tone = status.tone, modifier = modifier)
}

/** What a status is called, wherever it is shown. */
@get:StringRes
val OrderStatus.label: Int
    get() = when (this) {
        OrderStatus.Placed -> R.string.orders_status_placed
        OrderStatus.Packed -> R.string.orders_status_packed
        OrderStatus.Shipped -> R.string.orders_status_shipped
        OrderStatus.Delivered -> R.string.orders_status_delivered
        OrderStatus.Cancelled -> R.string.orders_status_cancelled
    }

private val OrderStatus.tone: TagTone
    get() = when (this) {
        OrderStatus.Placed, OrderStatus.Packed -> TagTone.Neutral
        OrderStatus.Shipped -> TagTone.Info
        OrderStatus.Delivered -> TagTone.Positive
        OrderStatus.Cancelled -> TagTone.Negative
    }

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    OrderStatus.entries.forEach { OrderStatusTag(status = it) }
}
