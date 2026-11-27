package com.example.androidproject1.feature.movies.presentation.movies

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.movies.domain.Movie
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * What the list screen keeps besides its rows: the long-pressed movie whose sheet is open.
 *
 * The rows are not here. They are Paging's — a `Flow<PagingData<Movie>>` the destination
 * collects — and so are the loading, the page and the end: state that a `PagingData` already is,
 * kept in a second place, would be the bookkeeping this replaced.
 */
@Immutable
data class MoviesState(
    /** The row that was long-pressed, while its action sheet is open; null otherwise. */
    val actionMovie: Movie? = null,
    /** When the list was fetched, in the device's zone; `null` while nothing is cached. */
    val lastUpdated: LocalDateTime? = null,
    /** Whether [lastUpdated] was today, so the screen says a time rather than a date. */
    val lastUpdatedToday: Boolean = true,
) {

    companion object {

        /** Three rows, as the previews and the screen tests hand the list. */
        val PREVIEW_MOVIES = listOf(
            Movie(
                id = 550,
                title = "Fight Club",
                overview = "An insomniac office worker and a soap salesman start an underground club.",
                posterUrl = null,
                releaseDate = LocalDate.of(1999, 10, 15),
                rating = 8.4,
            ),
            Movie(
                id = 27205,
                title = "Inception",
                overview = "A thief who steals secrets from inside dreams is offered a chance at redemption.",
                posterUrl = null,
                releaseDate = LocalDate.of(2010, 7, 15),
                rating = 8.4,
            ),
            Movie(
                id = 157336,
                title = "Interstellar",
                overview = "A team of explorers travels through a wormhole in search of a new home.",
                posterUrl = null,
                releaseDate = LocalDate.of(2014, 11, 5),
                rating = 8.4,
            ),
        )

        val PREVIEW = MoviesState()
    }
}
