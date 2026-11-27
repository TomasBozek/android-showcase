package com.example.androidproject1.feature.checkout.data.repository

import com.example.androidproject1.feature.cart.domain.CartItem
import com.example.androidproject1.feature.cart.domain.test.FakeCartRepository
import com.example.androidproject1.feature.checkout.data.source.DefaultRemoteCheckoutDataSource
import com.example.androidproject1.feature.checkout.domain.Address
import com.example.androidproject1.feature.checkout.domain.CheckoutDraft
import com.example.androidproject1.feature.checkout.domain.DeclineReason
import com.example.androidproject1.feature.checkout.domain.DeliveryOption
import com.example.androidproject1.feature.checkout.domain.InvalidPromo
import com.example.androidproject1.feature.checkout.domain.PaymentCard
import com.example.androidproject1.feature.checkout.domain.PaymentDeclined
import com.example.androidproject1.feature.checkout.domain.Promo
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.domain.test.FakeOrdersRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.domain.test.TestDispatchers
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.network.HttpClientFactory
import com.example.androidproject1.service.network.NetworkConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** An order placed empties the cart; one the bank declines, or the server refuses, leaves it be. */
class DefaultCheckoutRepositoryTest {

    private val lines = listOf(
        CartItem(productId = "coffee", name = "Coffee", price = 450, quantity = 2),
        CartItem(productId = "bread", name = "Bread", price = 300, quantity = 1),
    )
    private val cart = FakeCartRepository(initial = lines)
    private val orders = FakeOrdersRepository()
    private val requests = mutableListOf<Pair<HttpRequestData, String>>()

    private val address = Address("Ada Lovelace", "Na Příkopě 1", "Praha", "110 00", "CZ", "+420777123456")
    private val standard = DeliveryOption(id = "standard", name = "Standard", price = 0, days = 3)
    private val draft = CheckoutDraft(
        address = address,
        delivery = standard,
        card = PaymentCard(number = "4242424242424242", expiry = "1230", code = "123"),
    )

    private fun repository(answer: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        DefaultCheckoutRepository(
            logger = FakeLogger(),
            remoteCheckoutDataSource = DefaultRemoteCheckoutDataSource(
                client = HttpClientFactory.create(
                    engine = MockEngine { request ->
                        requests += request to String(request.body.toByteArray())
                        answer(request)
                    },
                    config = NetworkConfig(baseUrl = "https://fixtures.test/"),
                ),
                dispatcherProvider = TestDispatchers,
            ),
            cartRepository = cart,
            ordersRepository = orders,
        )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf("Content-Type", ContentType.Application.Json.toString()))

    @Test
    fun `a placed order is for the cart's lines, sends a token, clears the cart and is recorded`() = runTest {
        val repository = repository {
            json(
                """{"id":"A7K2Q9","total":1200,"discount":0,"placedAt":"2026-09-28T10:15:30Z"}""",
                HttpStatusCode.Created,
            )
        }

        val placed = repository.placeOrder(draft)

        val order = (placed as Outcome.Success).data
        assertEquals("A7K2Q9", order.id)
        assertEquals(lines, order.lines)
        assertEquals(1200L, order.total)
        assertEquals(Instant.parse("2026-09-28T10:15:30Z"), order.placedAt)
        assertEquals(standard, order.delivery)
        assertTrue(cart.current.isEmpty())
        val recorded = orders.current.single()
        assertEquals("A7K2Q9", recorded.id)
        assertEquals(OrderStatus.Placed, recorded.status)
        assertEquals(listOf("Coffee", "Bread"), recorded.lines.map { it.name })
        assertEquals(standard.name, recorded.deliveryName)

        val (request, body) = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/orders", request.url.encodedPath)
        assertTrue(body, "\"cardToken\":\"tok_4242424242424242\"" in body)
        assertFalse(body, "\"number\"" in body)
        assertTrue(body, "\"productId\":\"coffee\",\"quantity\":2" in body)
        assertTrue(body, "\"deliveryId\":\"standard\"" in body)
    }

    @Test
    fun `a Google Pay token is what is sent, with no card in the draft at all`() = runTest {
        val repository = repository {
            json(
                """{"id":"A7K2Q9","total":1200,"discount":0,"placedAt":"2026-09-28T10:15:30Z"}""",
                HttpStatusCode.Created,
            )
        }

        val placed = repository.placeOrder(draft.copy(card = null, paymentToken = "gpay-token-1"))

        assertTrue(placed is Outcome.Success)
        val (_, body) = requests.single()
        assertTrue(body, "\"cardToken\":\"gpay-token-1\"" in body)
    }

    @Test
    fun `a declined card is PaymentDeclined with its reason, and the cart is kept`() = runTest {
        val repository = repository {
            json("""{"reason":"insufficient_funds"}""", HttpStatusCode.PaymentRequired)
        }

        val placed = repository.placeOrder(draft)

        val error = (placed as Outcome.Failure).error
        assertTrue(error is PaymentDeclined)
        assertEquals(DeclineReason.InsufficientFunds, (error as PaymentDeclined).reason)
        assertEquals(lines, cart.current)
        assertTrue(orders.current.isEmpty())
    }

    @Test
    fun `a promo code the server does not know is InvalidPromo`() = runTest {
        val repository = repository { json("""{"error":"no such promo"}""", HttpStatusCode.NotFound) }

        val applied = repository.applyPromo(" welcome5 ")

        val error = (applied as Outcome.Failure).error
        assertTrue(error is InvalidPromo)
        assertEquals("WELCOME5", (error as InvalidPromo).code)
        assertEquals("WELCOME5", requests.single().first.url.parameters["code"])
    }

    @Test
    fun `a good promo code comes back with its discount, and travels with the order`() = runTest {
        val repository = repository { request ->
            if (request.url.encodedPath == "/promo") {
                json("""{"code":"WELCOME10","percentOff":10}""")
            } else {
                json(
                    """{"id":"B1","total":1080,"discount":120,"placedAt":"2026-09-28T10:15:30Z"}""",
                    HttpStatusCode.Created,
                )
            }
        }

        val promo = (repository.applyPromo("welcome10") as Outcome.Success).data
        val order = (repository.placeOrder(draft.copy(promo = promo)) as Outcome.Success).data

        assertEquals(Promo("WELCOME10", 10), promo)
        assertEquals(120L, promo.discountOn(1200))
        assertEquals(120L, order.discount)
        assertTrue(requests.last().second, "\"promoCode\":\"WELCOME10\"" in requests.last().second)
    }

    @Test
    fun `delivery options come from the server in its order`() = runTest {
        val repository = repository {
            json(
                """[{"id":"standard","name":"Standard","price":0,"days":3},{"id":"express","name":"Express","price":990,"days":1}]""",
            )
        }

        val options = (repository.deliveryOptions() as Outcome.Success).data

        assertEquals(listOf("standard", "express"), options.map { it.id })
        assertEquals(990L, options[1].price)
    }
}
