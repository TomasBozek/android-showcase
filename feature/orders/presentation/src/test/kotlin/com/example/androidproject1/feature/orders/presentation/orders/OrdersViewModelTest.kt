package com.example.androidproject1.feature.orders.presentation.orders

import com.example.androidproject1.feature.orders.domain.OrderStatus
import com.example.androidproject1.feature.orders.domain.test.FakeOrdersRepository
import com.example.androidproject1.feature.orders.domain.test.FakeOrdersRepository.Companion.order
import com.example.androidproject1.service.core.ui.test.MainDispatcherRule
import com.example.androidproject1.service.log.domain.test.FakeLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** The list: every kept order, grouped by status in the order an order moves, refreshed on open. */
class OrdersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeOrdersRepository(
        initial = listOf(
            order(id = "OLD", status = OrderStatus.Delivered, placedAt = Instant.parse("2026-09-01T10:00:00Z")),
            order(id = "NEW", status = OrderStatus.Placed, placedAt = Instant.parse("2026-09-28T10:00:00Z")),
            order(id = "GONE", status = OrderStatus.Cancelled, placedAt = Instant.parse("2026-08-01T10:00:00Z")),
            order(id = "ROAD", status = OrderStatus.Shipped, placedAt = Instant.parse("2026-09-20T10:00:00Z")),
        ),
    )

    private fun viewModel() = OrdersViewModel(logger = FakeLogger(), ordersRepository = repository)

    @Test
    fun `orders are grouped by status, in the order an order moves`() = runTest {
        val sections = viewModel().state.value.data!!.sections

        assertEquals(
            listOf(OrderStatus.Placed, OrderStatus.Shipped, OrderStatus.Delivered, OrderStatus.Cancelled),
            sections.map { it.status },
        )
        assertEquals(listOf("NEW"), sections.first().orders.map { it.id })
    }

    @Test
    fun `opening the list asks the server for what changed`() = runTest {
        viewModel()

        assertEquals(1, repository.refreshed)
    }

    @Test
    fun `a row opens its order`() = runTest {
        val viewModel = viewModel()

        viewModel.onUiEvent(OrdersEvent.OrderClicked("ROAD"))

        assertEquals(OrdersNavigation.Detail("ROAD"), viewModel.navigation.first())
    }

    @Test
    fun `no orders is the empty state, whose action leads to the catalog`() = runTest {
        val viewModel = OrdersViewModel(logger = FakeLogger(), ordersRepository = FakeOrdersRepository())

        assertTrue(viewModel.state.value.data!!.isEmpty)
        viewModel.onUiEvent(OrdersEvent.BrowseClicked)

        assertEquals(OrdersNavigation.Catalog, viewModel.navigation.first())
    }
}
