package com.example.androidproject1.feature.movies.presentation.moviefavourites

/** One-off navigation intents, turned into back-stack calls in MovieFavouritesDestination. */
sealed interface MovieFavouritesNavigation {

    data class MovieDetail(val id: Int) : MovieFavouritesNavigation

    data object NavigateUp : MovieFavouritesNavigation
}
