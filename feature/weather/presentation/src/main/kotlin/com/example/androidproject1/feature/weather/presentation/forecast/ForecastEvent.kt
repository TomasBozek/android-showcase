package com.example.androidproject1.feature.weather.presentation.forecast

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface ForecastEvent : UiEvent {

    data class TabSelected(val tab: ForecastTab) : ForecastEvent

    data object Refresh : ForecastEvent

    data object NavigateUpClicked : ForecastEvent

    /** Keeps a place the screen was opened on without it being saved — a link's. */
    data object SaveClicked : ForecastEvent
}
