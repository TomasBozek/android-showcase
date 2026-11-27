package com.example.androidproject1.feature.orders.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.feature.orders.data.database.OrdersDatabase
import com.example.androidproject1.feature.orders.data.source.DefaultLocalOrdersDataSource
import com.example.androidproject1.feature.orders.data.source.DefaultRemoteOrdersDataSource
import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.domain.StatusChange
import com.example.androidproject1.feature.orders.domain.test.FakeOrdersRepository.Companion.order
import com.example.androidproject1.service.core.domain.error.NotFoundError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.domain.test.TestDispatchers
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.network.HttpClientFactory
import com.example.androidproject1.service.network.NetworkConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** Orders kept in Room: what checkout records, what a refresh adds, and each history in order. */
@RunWith(RobolectricTestRunner::class)
class DefaultOrdersRepositoryTest {

    private lateinit var database: OrdersDatabase
    private var serverOrders = "[]"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OrdersDatabase::class.java)
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    private val repository by lazy {
        DefaultOrdersRepository(
            logger = FakeLogger(),
            localOrdersDataSource = DefaultLocalOrdersDataSource(database.ordersDao()),
            remoteOrdersDataSource = DefaultRemoteOrdersDataSource(
                client = HttpClientFactory.create(
                    engine = MockEngine {
                        respond(
                            serverOrders,
                            HttpStatusCode.OK,
                            headersOf("Content-Type", ContentType.Application.Json.toString()),
                        )
                    },
                    config = NetworkConfig(baseUrl = "https://fixtures.test/"),
                ),
                dispatcherProvider = TestDispatchers,
            ),
        )
    }

    @Test
    fun `a recorded order is listed, whole, newest first`() = runTest {
        val older = order(id = "A1", placedAt = Instant.parse("2026-09-27T10:00:00Z"))
        val newer = order(id = "A2", placedAt = Instant.parse("2026-09-28T10:00:00Z"))

        repository.record(older)
        repository.record(newer)

        val listed = (repository.observeOrders().first() as Outcome.Success).data
        assertEquals(listOf("A2", "A1"), listed.map { it.id })
        assertEquals(newer, listed.first())
    }

    @Test
    fun `a refresh appends the changes the server has, in order, and keeps what it does not know`() = runTest {
        repository.record(order(id = "A1"))
        repository.record(order(id = "LOCAL"))
        serverOrders = """[${serverOrder(
            "A1",
            """[{"status":"shipped","at":"2026-09-28T12:00:00Z"},""" +
                """{"status":"placed","at":"2026-09-28T10:00:00Z"},{"status":"packed","at":"2026-09-28T11:00:00Z"}]""",
        )}]"""

        repository.refresh()

        val kept = (repository.observeOrder("A1").first() as Outcome.Success).data
        assertEquals(OrderStatus.Shipped, kept.status)
        assertEquals(
            listOf(
                StatusChange(OrderStatus.Placed, Instant.parse("2026-09-28T10:00:00Z")),
                StatusChange(OrderStatus.Packed, Instant.parse("2026-09-28T11:00:00Z")),
                StatusChange(OrderStatus.Shipped, Instant.parse("2026-09-28T12:00:00Z")),
            ),
            kept.history,
        )
        assertTrue(repository.observeOrder("LOCAL").first() is Outcome.Success)
    }

    @Test
    fun `a refresh adds an order placed somewhere else`() = runTest {
        serverOrders = """[${serverOrder(
            "B7",
            """[{"status":"placed","at":"2026-09-26T09:00:00Z"},""" +
                """{"status":"cancelled","at":"2026-09-26T09:30:00Z"}]""",
        )}]"""

        repository.refresh()

        val added = (repository.observeOrder("B7").first() as Outcome.Success).data
        assertEquals(OrderStatus.Cancelled, added.status)
        assertEquals("Coffee", added.lines.single().name)
        assertEquals("Praha", added.address.city)
    }

    @Test
    fun `an order nobody kept is NotFoundError`() = runTest {
        val missing = repository.observeOrder("NOPE").first()

        assertTrue((missing as Outcome.Failure).error is NotFoundError)
    }

    private fun serverOrder(id: String, history: String) =
        """{"id":"$id","placedAt":"2026-09-26T09:00:00Z",""" +
            """"lines":[{"productId":"coffee","name":"Coffee","price":450,"quantity":2}],""" +
            """"address":{"name":"Ada","street":"Na Příkopě 1","city":"Praha","postalCode":"110 00",""" +
            """"country":"CZ","phone":"+420777123456"},"deliveryName":"Standard courier","deliveryPrice":490,""" +
            """"discount":0,"total":1390,"history":$history}"""
}
