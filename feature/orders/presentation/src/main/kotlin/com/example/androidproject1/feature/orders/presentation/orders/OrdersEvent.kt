package com.example.androidproject1.feature.orders.presentation.orders

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface OrdersEvent : UiEvent {

    data class OrderClicked(val orderId: String) : OrdersEvent

    /** The empty state's action: nothing ordered yet, so go and find something. */
    data object BrowseClicked : OrdersEvent

    data object UpClicked : OrdersEvent
}
