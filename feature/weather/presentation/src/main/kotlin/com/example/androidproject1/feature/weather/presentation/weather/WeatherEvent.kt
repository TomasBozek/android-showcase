package com.example.androidproject1.feature.weather.presentation.weather

import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface WeatherEvent : UiEvent {

    data class QueryChanged(val query: String) : WeatherEvent

    /** A geocoder result tapped: saved, and opened. */
    data class ResultClicked(val place: Place) : WeatherEvent

    data class PlaceClicked(val placeId: Int) : WeatherEvent

    /** One of the offered places tapped: saved and opened, the way a search result is. */
    data class SuggestedPlaceClicked(val place: Place) : WeatherEvent

    data class PlaceRemoved(val placeId: Int) : WeatherEvent

    /** A row dragged by its handle to a new place; the repository renumbers the rest. */
    data class PlaceMoved(val placeId: Int, val toIndex: Int) : WeatherEvent

    data class UnitSelected(val unit: TemperatureUnit) : WeatherEvent

    /** A long press on a row: the first one enters selection mode. */
    data class PlaceLongPressed(val placeId: Int) : WeatherEvent

    data class SelectAllChanged(val checked: Boolean) : WeatherEvent

    data object RemoveSelectedClicked : WeatherEvent

    data object SelectionCleared : WeatherEvent

    /** "Use my location", shown once the permission is held: one fix, named, saved and opened. */
    data object MyLocationTapped : WeatherEvent

    /** The rationale's button once the system will not ask again: the app's settings page. */
    data object OpenAppSettingsClicked : WeatherEvent

    data object NavigateUpClicked : WeatherEvent
}
