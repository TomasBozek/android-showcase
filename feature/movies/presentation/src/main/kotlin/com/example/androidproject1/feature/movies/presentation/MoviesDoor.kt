package com.example.androidproject1.feature.movies.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.shell.figure
import com.example.androidproject1.feature.movies.api.MovieDetailDestination
import com.example.androidproject1.feature.movies.api.MovieFavouritesDestination
import com.example.androidproject1.feature.movies.api.MoviesDestination
import com.example.androidproject1.feature.movies.domain.MovieFavouritesRepository
import com.example.androidproject1.service.core.ui.text.toPluralUiText

/** Movies's door: Shows, counting the hearted ones. */
fun moviesDoor(repository: MovieFavouritesRepository): ShellDoor = ShellDoor(
    id = "movies",
    section = ShellSection.Yours,
    title = R.string.movies_door_title,
    icon = Icons.Filled.PlayArrow,
    route = MoviesDestination,
    placements = setOf(ShellPlacement.Home, ShellPlacement.Menu),
    order = 3,
    figure = repository.observeFavourites().figure { R.plurals.movies_door_count.toPluralUiText(it.size, it.size) },
)

/**
 * The screens of movies the debug menu opens in one tap; a route with an argument gets a
 * value the fixtures serve on every flavor.
 */
val moviesJumps: List<ShellJump> = listOf(
    ShellJump(id = "movies", screenId = "MoviesScreen", route = MoviesDestination),
    ShellJump(
        id = "movieDetail",
        screenId = "MovieDetailScreen",
        route = MovieDetailDestination(movieId = FIXTURE_MOVIE_ID),
    ),
    ShellJump(id = "movieFavourites", screenId = "MovieFavouritesScreen", route = MovieFavouritesDestination),
)

/** Under the Dome, the first show of the `dev` flavor's first page fixture (TVmaze ids). */
private const val FIXTURE_MOVIE_ID = 1
