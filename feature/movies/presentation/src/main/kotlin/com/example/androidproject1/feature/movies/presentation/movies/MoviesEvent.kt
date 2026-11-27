package com.example.androidproject1.feature.movies.presentation.movies

import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface MoviesEvent : UiEvent {

    /** A refresh failed over rows already on screen: the view model says how old they are. */
    data object RefreshFailed : MoviesEvent

    /** The list was pulled down. Answered where the paging items are — the destination — not by the view model. */
    data object Refresh : MoviesEvent

    /** A failed load's "try again": the footer's, the stale row's or the error page's. Answered like [Refresh]. */
    data object Retry : MoviesEvent

    data class MovieClicked(val id: Int) : MoviesEvent

    /** A row was long-pressed: its actions open in a sheet. */
    data class MovieLongPressed(val movie: Movie) : MoviesEvent

    /** The sheet was closed without choosing, or after a choice. */
    data object ActionSheetDismissed : MoviesEvent

    /** "Add to favourites" on the long-pressed row's sheet. */
    data object FavouriteFromSheetClicked : MoviesEvent

    data object FavouritesClicked : MoviesEvent

    data object NavigateUpClicked : MoviesEvent
}
