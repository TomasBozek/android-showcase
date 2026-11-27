package com.example.androidproject1.feature.orders.presentation.orders

import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.layout.listPane
import com.example.androidproject1.feature.catalog.api.CategoriesDestination
import com.example.androidproject1.feature.orders.api.OrderDetailDestination
import com.example.androidproject1.feature.orders.api.OrdersDestination
import com.example.androidproject1.feature.orders.presentation.R
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

/** Profile's "Your orders" pushes [OrdersDestination] from another feature. */
fun EntryProviderScope<NavKey>.ordersDestination(navigator: Navigator) {
    // The list half of a list–detail pair: a wide window shows the detail beside it, and
    // the placeholder until something is picked; a phone is unchanged.
    entry<OrdersDestination>(
        metadata = listPane(
            detailPlaceholder = {
                AppEmptyState(
                    title = stringResource(R.string.order_detail_placeholder_title),
                    message = stringResource(R.string.order_detail_placeholder_message),
                )
            },
        ),
    ) {
        val viewModel: OrdersViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    is OrdersNavigation.Detail -> navigator.push(OrderDetailDestination(orderId = navigation.orderId))
                    OrdersNavigation.Catalog -> navigator.selectTab(CategoriesDestination)
                    OrdersNavigation.Back -> navigator.back()
                }
            },
        ) { state, onEvent ->
            OrdersScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
