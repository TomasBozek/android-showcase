package com.example.androidproject1.feature.orders.data.source

import com.example.androidproject1.feature.orders.domain.Order
import com.example.androidproject1.feature.orders.domain.OrderAddress
import com.example.androidproject1.feature.orders.domain.OrderLine
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.domain.StatusChange
import kotlinx.serialization.Serializable
import java.time.Instant

/** `GET orders`: the wire shape, deliberately not the domain's. Times are ISO-8601, UTC. */
@Serializable
data class OrderDto(
    val id: String,
    val placedAt: String,
    val lines: List<OrderLineDto>,
    val address: OrderAddressDto,
    val deliveryName: String,
    val deliveryPrice: Long,
    val discount: Long = 0,
    val total: Long,
    val history: List<StatusChangeDto>,
)

@Serializable
data class OrderLineDto(val productId: String, val name: String, val price: Long, val quantity: Int)

@Serializable
data class OrderAddressDto(
    val name: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val phone: String,
)

/** `status` is lower case on the wire: `placed`, `packed`, `shipped`, `delivered`, `cancelled`. */
@Serializable
data class StatusChangeDto(val status: String, val at: String)

/** A change whose status this build does not know is dropped, not a failed refresh. */
internal fun OrderDto.toDomain() = Order(
    id = id,
    placedAt = Instant.parse(placedAt),
    lines = lines.map { OrderLine(it.productId, it.name, it.price, it.quantity) },
    address = with(address) { OrderAddress(name, street, city, postalCode, country, phone) },
    deliveryName = deliveryName,
    deliveryPrice = deliveryPrice,
    discount = discount,
    total = total,
    history = history.mapNotNull { change ->
        OrderStatus.entries.firstOrNull { it.name.equals(change.status, ignoreCase = true) }
            ?.let { StatusChange(it, Instant.parse(change.at)) }
    }.sortedBy { it.at },
)
