package com.example.androidproject1.feature.movies.presentation.movies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppPullToRefresh
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.overlay.AppActionSheet
import com.example.androidproject1.core.ui.component.overlay.MenuItem
import com.example.androidproject1.core.ui.component.status.AppSpinner
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.feature.movies.presentation.R
import com.example.androidproject1.feature.movies.presentation.component.MovieList
import com.example.androidproject1.service.locale.LocalFormats
import kotlinx.coroutines.flow.flowOf

@Composable
fun MoviesScreen(
    state: MoviesState,
    movies: LazyPagingItems<Movie>,
    onEvent: (MoviesEvent) -> Unit,
) {
    val refresh = movies.loadState.refresh
    // A refresh that failed over rows is reported once per failure, for the view model to say how
    // old the rows are; a first load that failed has its own page and needs no word.
    val failedOverRows = refresh is LoadState.Error && movies.itemCount > 0
    LaunchedEffect(refresh) { if (failedOverRows) onEvent(MoviesEvent.RefreshFailed) }
    AppScaffold(
        screenId = "MoviesScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.movies_title),
                onNavigateUp = { onEvent(MoviesEvent.NavigateUpClicked) },
                navigateUpTestTag = "movies_upButton",
                actions = {
                    AppIconButton(
                        icon = Icons.Filled.Favorite,
                        contentDescription = stringResource(R.string.movies_favourites),
                        onClick = { onEvent(MoviesEvent.FavouritesClicked) },
                        modifier = Modifier.testTag("movies_favouritesButton"),
                    )
                },
            )
        },
        contentPadding = false,
    ) {
        state.lastUpdated?.let { at ->
            val formats = LocalFormats.current
            AppText(
                text = stringResource(
                    R.string.movies_last_updated,
                    if (state.lastUpdatedToday) formats.time(at.toLocalTime()) else formats.date(at.toLocalDate()),
                ),
                role = TextRole.Secondary,
                modifier = Modifier
                    .padding(horizontal = AppTheme.spacing.inset.lg, vertical = AppTheme.spacing.inset.sm)
                    .testTag("movies_lastUpdatedValue"),
            )
        }
        AppPullToRefresh(
            // The box's own state is for a refresh over rows; the first load has a spinner of its own.
            refreshing = refresh is LoadState.Loading && movies.itemCount > 0,
            onRefresh = { onEvent(MoviesEvent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                // Nothing to show yet: the first load, or a first load that failed with nothing cached.
                movies.itemCount == 0 && refresh is LoadState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("movies_loadingProgress"),
                    contentAlignment = Alignment.Center,
                ) {
                    AppSpinner()
                }

                movies.itemCount == 0 && refresh is LoadState.Error -> AppEmptyState(
                    title = stringResource(R.string.movies_error_title),
                    message = stringResource(R.string.movies_error_message),
                    actionLabel = stringResource(R.string.movies_retry),
                    onAction = { onEvent(MoviesEvent.Retry) },
                    actionTestTag = "movies_retryButton",
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("movies_errorGroup"),
                )

                movies.itemCount == 0 -> AppEmptyState(
                    title = stringResource(R.string.movies_empty_title),
                    message = stringResource(R.string.movies_empty_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("movies_empty"),
                )

                else -> MovieList(
                    movies = movies,
                    onMovieClick = { onEvent(MoviesEvent.MovieClicked(it.id)) },
                    onMovieLongClick = { onEvent(MoviesEvent.MovieLongPressed(it)) },
                    onRetry = { onEvent(MoviesEvent.Retry) },
                )
            }
        }

        // The long-pressed row's actions: open it, or heart it without opening it. The
        // sheet is the row's state, so a rotation keeps it open.
        state.actionMovie?.let { movie ->
            AppActionSheet(
                title = movie.title,
                onDismiss = { onEvent(MoviesEvent.ActionSheetDismissed) },
                items = listOf(
                    MenuItem(
                        label = stringResource(R.string.movies_action_open),
                        icon = Icons.Filled.PlayArrow,
                        testTag = "movies_openItem",
                    ) { onEvent(MoviesEvent.MovieClicked(movie.id)) },
                    MenuItem(
                        label = stringResource(R.string.movies_action_favourite),
                        icon = Icons.Filled.Favorite,
                        testTag = "movies_favouriteItem",
                    ) { onEvent(MoviesEvent.FavouriteFromSheetClicked) },
                ),
                modifier = Modifier.testTag("movies_actionSheet"),
            )
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    MoviesScreen(
        state = MoviesState.PREVIEW,
        movies = flowOf(PagingData.from(MoviesState.PREVIEW_MOVIES)).collectAsLazyPagingItems(),
    ) {}
}

@ScreenPreview
@Composable
private fun EmptyPreview() = ThemedScreenPreview {
    MoviesScreen(
        state = MoviesState.PREVIEW,
        movies = flowOf(PagingData.empty<Movie>()).collectAsLazyPagingItems(),
    ) {}
}

@ScreenPreview
@Composable
private fun PageOnItsWayPreview() = ThemedScreenPreview {
    val loading = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.Loading)
    MoviesScreen(
        state = MoviesState.PREVIEW,
        movies = flowOf(PagingData.from(MoviesState.PREVIEW_MOVIES, loading)).collectAsLazyPagingItems(),
    ) {}
}

@ScreenPreview
@Composable
private fun PageFailedPreview() = ThemedScreenPreview {
    val failed = LoadStates(
        LoadState.NotLoading(false),
        LoadState.NotLoading(true),
        LoadState.Error(java.io.IOException("offline")),
    )
    MoviesScreen(
        state = MoviesState.PREVIEW,
        movies = flowOf(PagingData.from(MoviesState.PREVIEW_MOVIES, failed)).collectAsLazyPagingItems(),
    ) {}
}
