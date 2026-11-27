package com.example.androidproject1.feature.orders.presentation.orders

/** One-off navigation intents, turned into back-stack calls in OrdersDestination. */
sealed interface OrdersNavigation {

    data class Detail(val orderId: String) : OrdersNavigation

    /** The catalog's tab, from the empty state. */
    data object Catalog : OrdersNavigation

    data object Back : OrdersNavigation
}
