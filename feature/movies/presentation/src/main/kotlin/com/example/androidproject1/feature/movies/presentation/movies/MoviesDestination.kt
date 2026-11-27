package com.example.androidproject1.feature.movies.presentation.movies

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.layout.listPane
import com.example.androidproject1.feature.movies.api.MovieDetailDestination
import com.example.androidproject1.feature.movies.api.MovieFavouritesDestination
import com.example.androidproject1.feature.movies.api.MoviesDestination
import com.example.androidproject1.feature.movies.presentation.R
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.moviesDestination(navigator: Navigator) {
    // The list half of a list–detail pair: a wide window shows the detail beside it, and
    // the placeholder until something is picked; a phone is unchanged.
    entry<MoviesDestination>(
        metadata = listPane(
            detailPlaceholder = {
                AppEmptyState(
                    title = stringResource(R.string.movie_detail_placeholder_title),
                    message = stringResource(R.string.movie_detail_placeholder_message),
                )
            },
        ),
    ) {
        val viewModel: MoviesViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    is MoviesNavigation.MovieDetail -> navigator.push(MovieDetailDestination(movieId = navigation.id))
                    MoviesNavigation.Favourites -> navigator.push(MovieFavouritesDestination)
                    MoviesNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            // The rows are Paging's, and so are refresh and retry: the items that can do
            // either live here, so the destination answers those two and hands on the rest.
            val movies = viewModel.movies.collectAsLazyPagingItems()
            // A return after the cache's age limit: the view model decides, the items refresh.
            LaunchedEffect(viewModel, movies) { viewModel.refreshes.collect { movies.refresh() } }
            MoviesScreen(
                state = state,
                movies = movies,
                onEvent = { event ->
                    when (event) {
                        MoviesEvent.Refresh -> movies.refresh()
                        MoviesEvent.Retry -> movies.retry()
                        else -> onEvent(event)
                    }
                },
            )
        }
    }
}
