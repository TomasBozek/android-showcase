package com.example.androidproject1.feature.movies.presentation.moviefavourites

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.feature.movies.presentation.movies.MoviesState

/** What was hearted, newest first, straight from the database. */
@Immutable
data class MovieFavouritesState(
    val movies: List<Movie> = emptyList(),
    /** The first emission has arrived, so an empty list means empty rather than not yet read. */
    val loaded: Boolean = false,
) {

    companion object {

        val PREVIEW = MovieFavouritesState(
            movies = MoviesState.PREVIEW_MOVIES,
            loaded = true,
        )
    }
}

/** The states this screen is drawn in: the list, and the empty state. */
class MovieFavouritesStatePreviews : PreviewParameterProvider<MovieFavouritesState> {

    override val values = sequenceOf(
        MovieFavouritesState.PREVIEW,
        MovieFavouritesState(loaded = true),
    )
}
