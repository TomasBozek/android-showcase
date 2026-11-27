package com.example.androidproject1.feature.movies.presentation.moviefavourites

import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.layout.listPane
import com.example.androidproject1.feature.movies.api.MovieDetailDestination
import com.example.androidproject1.feature.movies.api.MovieFavouritesDestination
import com.example.androidproject1.feature.movies.presentation.R
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.movieFavouritesDestination(navigator: Navigator) {
    // The list half of a list–detail pair: a wide window shows the detail beside it, and
    // the placeholder until something is picked; a phone is unchanged.
    entry<MovieFavouritesDestination>(
        metadata = listPane(
            detailPlaceholder = {
                AppEmptyState(
                    title = stringResource(R.string.movie_detail_placeholder_title),
                    message = stringResource(R.string.movie_detail_placeholder_message),
                )
            },
        ),
    ) {
        val viewModel: MovieFavouritesViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            // Turn each MovieFavouritesNavigation case into a navigator call here — `navigator.push(Key)`
            // to push, `navigator.back()` to pop. Another feature is reached the same way, by the
            // route key in its api module.
            onNavigation = { navigation ->
                when (navigation) {
                    is MovieFavouritesNavigation.MovieDetail -> navigator.push(
                        MovieDetailDestination(movieId = navigation.id),
                    )
                    MovieFavouritesNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            MovieFavouritesScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
