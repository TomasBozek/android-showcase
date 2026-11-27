package com.example.androidproject1.feature.orders.presentation.orderdetail

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface OrderDetailEvent : UiEvent {

    data object UpClicked : OrderDetailEvent
}
