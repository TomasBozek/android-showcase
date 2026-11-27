package com.example.androidproject1.feature.orders.data.database

import com.example.androidproject1.feature.orders.domain.Order
import com.example.androidproject1.feature.orders.domain.OrderAddress
import com.example.androidproject1.feature.orders.domain.OrderLine
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.domain.StatusChange
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

/** A line as the `lines` column holds it. Its own type, so renaming a domain field migrates nothing. */
@Serializable
private data class StoredLine(val productId: String, val name: String, val price: Long, val quantity: Int)

private val json = Json { ignoreUnknownKeys = true }

internal fun Order.toEntity() = OrderEntity(
    id = id,
    placedAt = placedAt.toEpochMilli(),
    lines = json.encodeToString(lines.map { StoredLine(it.productId, it.name, it.price, it.quantity) }),
    name = address.name,
    street = address.street,
    city = address.city,
    postalCode = address.postalCode,
    country = address.country,
    phone = address.phone,
    deliveryName = deliveryName,
    deliveryPrice = deliveryPrice,
    discount = discount,
    total = total,
)

internal fun Order.historyEntities() = history.map {
    StatusChangeEntity(orderId = id, status = it.status.name, at = it.at.toEpochMilli())
}

/**
 * Back to the domain, the history sorted by time — the rows come back in whatever order SQLite
 * keeps them. A status this build does not know is dropped rather than failing the list; a
 * history left empty reads as placed when the order was.
 */
internal fun OrderWithHistory.toDomain(): Order {
    val placedAt = Instant.ofEpochMilli(order.placedAt)
    val changes = history
        .mapNotNull { row ->
            OrderStatus.entries.firstOrNull {
                it.name == row.status
            }?.let { StatusChange(it, Instant.ofEpochMilli(row.at)) }
        }
        .sortedWith(compareBy<StatusChange> { it.at }.thenBy { it.status.ordinal })
        .ifEmpty { listOf(StatusChange(OrderStatus.Placed, placedAt)) }
    return Order(
        id = order.id,
        placedAt = placedAt,
        lines = json.decodeFromString<List<StoredLine>>(order.lines).map {
            OrderLine(it.productId, it.name, it.price, it.quantity)
        },
        address = OrderAddress(order.name, order.street, order.city, order.postalCode, order.country, order.phone),
        deliveryName = order.deliveryName,
        deliveryPrice = order.deliveryPrice,
        discount = order.discount,
        total = order.total,
        history = changes,
    )
}
