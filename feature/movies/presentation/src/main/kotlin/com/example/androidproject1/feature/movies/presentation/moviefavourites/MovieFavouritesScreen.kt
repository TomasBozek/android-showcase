package com.example.androidproject1.feature.movies.presentation.moviefavourites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.list.SwipeAction
import com.example.androidproject1.core.ui.component.list.SwipeActions
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.feature.movies.presentation.R
import com.example.androidproject1.feature.movies.presentation.component.MovieRow

@Composable
fun MovieFavouritesScreen(
    state: MovieFavouritesState,
    onEvent: (MovieFavouritesEvent) -> Unit,
) {
    AppScaffold(
        screenId = "MovieFavouritesScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.movie_favourites_title),
                onNavigateUp = { onEvent(MovieFavouritesEvent.NavigateUpClicked) },
                navigateUpTestTag = "movieFavourites_upButton",
            )
        },
        contentPadding = false,
    ) {
        when {
            state.loaded && state.movies.isEmpty() -> AppEmptyState(
                title = stringResource(R.string.movie_favourites_empty_title),
                message = stringResource(R.string.movie_favourites_empty_message),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("movieFavourites_empty"),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("movieFavourites_list"),
            ) {
                items(state.movies, key = { it.id }) { movie ->
                    // The same row as the list's: a favourite is a movie, not a different thing.
                    // A swipe reveals remove; the detail's heart is the other way out.
                    MovieRow(
                        movie = movie,
                        onClick = { onEvent(MovieFavouritesEvent.MovieClicked(movie.id)) },
                        swipeActions = SwipeActions(
                            end = SwipeAction(
                                label = stringResource(R.string.movie_favourites_remove),
                                icon = Icons.Filled.Delete,
                                tone = TagTone.Negative,
                                testTag = "movieFavourites_itemRemoveButton",
                                onAction = { onEvent(MovieFavouritesEvent.RemoveClicked(movie.id)) },
                            ),
                        ),
                        modifier = appAnimateItem().testTag("movieFavourites_item_${movie.id}"),
                    )
                    AppDivider(modifier = appAnimateItem())
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview(
    @PreviewParameter(MovieFavouritesStatePreviews::class) state: MovieFavouritesState,
) = ThemedScreenPreview {
    MovieFavouritesScreen(
        state = state,
    ) {}
}
