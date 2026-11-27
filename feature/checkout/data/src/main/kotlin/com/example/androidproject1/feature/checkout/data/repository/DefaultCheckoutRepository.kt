package com.example.androidproject1.feature.checkout.data.repository

import com.example.androidproject1.feature.cart.domain.CartRepository
import com.example.androidproject1.feature.checkout.data.source.OrderLineDto
import com.example.androidproject1.feature.checkout.data.source.PlaceOrderRequest
import com.example.androidproject1.feature.checkout.data.source.RemoteCheckoutDataSource
import com.example.androidproject1.feature.checkout.data.source.toDomain
import com.example.androidproject1.feature.checkout.data.source.toDto
import com.example.androidproject1.feature.checkout.domain.CheckoutDraft
import com.example.androidproject1.feature.checkout.domain.CheckoutRepository
import com.example.androidproject1.feature.checkout.domain.DeliveryOption
import com.example.androidproject1.feature.checkout.domain.PaymentCard
import com.example.androidproject1.feature.checkout.domain.PlacedOrder
import com.example.androidproject1.feature.checkout.domain.Promo
import com.example.androidproject1.feature.orders.domain.Order
import com.example.androidproject1.feature.orders.domain.OrderAddress
import com.example.androidproject1.feature.orders.domain.OrderLine
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.domain.OrdersRepository
import com.example.androidproject1.feature.orders.domain.StatusChange
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.error.BadRequestError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Checkout against the server, and the cart it empties. The order is for the cart's lines as
 * they are when it is placed — read here, not carried through the wizard, so a line removed on the
 * way is not charged — and the cart is cleared only once the server has taken the order, which is
 * also when the order is recorded for the orders feature.
 */
class DefaultCheckoutRepository(
    logger: Logger,
    private val remoteCheckoutDataSource: RemoteCheckoutDataSource,
    private val cartRepository: CartRepository,
    private val ordersRepository: OrdersRepository,
) : CheckoutRepository, BaseRepository(logger = logger.withTag("DefaultCheckoutRepository")) {

    override suspend fun deliveryOptions(): Outcome<List<DeliveryOption>> = execute {
        remoteCheckoutDataSource.getDeliveryOptions().map { it.toDomain() }
    }

    override suspend fun applyPromo(code: String): Outcome<Promo> = execute {
        remoteCheckoutDataSource.getPromo(code.trim().uppercase())
    }

    override suspend fun placeOrder(draft: CheckoutDraft): Outcome<PlacedOrder> = execute {
        val address = requireNotNull(draft.address)
        val delivery = requireNotNull(draft.delivery)
        val lines = when (val items = cartRepository.observeItems().first()) {
            is Outcome.Success -> items.data
            is Outcome.Failure -> throw items.error
        }
        if (lines.isEmpty()) throw BadRequestError(message = "the cart is empty")

        val placed = remoteCheckoutDataSource.placeOrder(
            PlaceOrderRequest(
                lines = lines.map { OrderLineDto(productId = it.productId, quantity = it.quantity) },
                address = address.toDto(),
                deliveryId = delivery.id,
                cardToken = draft.paymentToken ?: requireNotNull(draft.card).token(),
                promoCode = draft.promo?.code,
            ),
        )
        // The order stands whether or not the cart empties: a failed clear is logged by the
        // cart's own repository, and the user sees a cart they can empty rather than a lost order.
        cartRepository.clear()
        val order = PlacedOrder(
            id = placed.id,
            lines = lines,
            address = address,
            delivery = delivery,
            discount = placed.discount,
            total = placed.total,
            placedAt = Instant.parse(placed.placedAt),
        )
        // Kept at once, so the order is listed before the server is asked again. The same
        // rule as the clear: a failed write is logged by the orders repository, the order stands,
        // and the next refresh brings it back from the server.
        ordersRepository.record(order.toOrder())
        order
    }

    /**
     * The card as the server takes it. A real integration hands the card to the payment
     * processor's SDK and sends the token it returns, so the number never reaches the shop's
     * server; the `dev` fixture has no processor, and its token is the number behind a prefix,
     * which is how it knows the published decline cards.
     */
    private fun PaymentCard.token(): String = "tok_$number"

    private fun PlacedOrder.toOrder() = Order(
        id = id,
        placedAt = placedAt,
        lines = lines.map {
            OrderLine(productId = it.productId, name = it.name, price = it.price, quantity = it.quantity)
        },
        address = with(address) { OrderAddress(name, street, city, postalCode, country, phone) },
        deliveryName = delivery.name,
        deliveryPrice = delivery.price,
        discount = discount,
        total = total,
        history = listOf(StatusChange(OrderStatus.Placed, placedAt)),
    )
}
