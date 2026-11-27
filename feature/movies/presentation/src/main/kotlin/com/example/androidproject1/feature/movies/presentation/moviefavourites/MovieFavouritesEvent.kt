package com.example.androidproject1.feature.movies.presentation.moviefavourites

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface MovieFavouritesEvent : UiEvent {

    data class MovieClicked(val id: Int) : MovieFavouritesEvent

    /** The key a swipe on a row reveals: the heart off, and the row leaves the list. */
    data class RemoveClicked(val id: Int) : MovieFavouritesEvent

    data object NavigateUpClicked : MovieFavouritesEvent
}
