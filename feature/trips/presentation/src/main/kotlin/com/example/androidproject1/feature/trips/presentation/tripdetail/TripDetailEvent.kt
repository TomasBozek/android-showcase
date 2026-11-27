package com.example.androidproject1.feature.trips.presentation.tripdetail

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface TripDetailEvent : UiEvent {

    data class TabSelected(val index: Int) : TripDetailEvent

    data object DeleteClicked : TripDetailEvent

    /** Open the destination in a map app. */
    data object MapClicked : TripDetailEvent

    /** Put the trip in the calendar: the calendar's own form, saved only by the user. */
    data object CalendarClicked : TripDetailEvent

    /** The reminder switch: a notification at 18:00 the evening before the trip. The screen has already got the permission. */
    data class ReminderToggled(val on: Boolean) : TripDetailEvent

    /** The Up arrow was tapped. Every non-root screen has one. */
    data object NavigateUpClicked : TripDetailEvent
}
