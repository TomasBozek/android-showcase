package com.example.androidproject1.feature.movies.presentation.moviedetail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.layout.detailPane
import com.example.androidproject1.feature.movies.api.MovieDetailDestination
import com.example.androidproject1.feature.movies.api.MovieFavouritesDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.movieDetailDestination(navigator: Navigator) {
    // The detail half of its list's pair.
    entry<MovieDetailDestination>(metadata = detailPane()) { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: MovieDetailViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    MovieDetailNavigation.NavigateUp -> navigator.back()
                    MovieDetailNavigation.OpenFavourites -> navigator.push(MovieFavouritesDestination)
                }
            },
        ) { state, onEvent ->
            MovieDetailScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
