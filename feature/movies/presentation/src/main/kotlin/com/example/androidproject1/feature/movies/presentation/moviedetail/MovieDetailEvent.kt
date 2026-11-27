package com.example.androidproject1.feature.movies.presentation.moviedetail

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface MovieDetailEvent : UiEvent {

    data object FavouriteToggled : MovieDetailEvent

    /** A link inside the overview: opened in a Custom Tab. */
    data class LinkClicked(val url: String) : MovieDetailEvent

    /** The poster: open it full screen. */
    data object PosterClicked : MovieDetailEvent

    /** The full-screen viewer was closed. */
    data object ViewerClosed : MovieDetailEvent

    data object NavigateUpClicked : MovieDetailEvent
}
