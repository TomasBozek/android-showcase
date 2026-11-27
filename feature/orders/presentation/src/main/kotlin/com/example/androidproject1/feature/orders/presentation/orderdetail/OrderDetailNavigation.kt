package com.example.androidproject1.feature.orders.presentation.orderdetail

/** One-off navigation intents, turned into back-stack calls in OrderDetailDestination. */
sealed interface OrderDetailNavigation {

    data object Back : OrderDetailNavigation
}
