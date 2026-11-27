package com.example.androidproject1.feature.movies.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.feature.movies.presentation.R
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/**
 * The rows, from Paging: they read as far as the list has scrolled and the pager asks for the
 * next page a few rows before the end, so there is no "load more" to send. Under the rows the
 * footer says what the next page is doing; above them, when the last refresh failed but rows are
 * cached, a line says so — the offline read — with its own retry.
 */
@Composable
fun MovieList(
    movies: LazyPagingItems<Movie>,
    onMovieClick: (Movie) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onMovieLongClick: ((Movie) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("movies_list"),
    ) {
        if (movies.loadState.refresh is LoadState.Error) {
            item(key = STALE_KEY) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spacing.inset.lg)
                        .testTag("movies_staleGroup"),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppText(text = stringResource(R.string.movies_stale), role = TextRole.Secondary)
                    AppButton(
                        label = stringResource(R.string.movies_retry),
                        onClick = onRetry,
                        kind = ButtonKind.Outline,
                        modifier = Modifier.testTag("movies_refreshRetryButton"),
                    )
                }
            }
        }
        items(count = movies.itemCount, key = movies.itemKey { it.id }) { index ->
            // A row that is still on its way to the table is `null`; with placeholders off that
            // is only the instant between a page being asked for and it arriving.
            movies[index]?.let { movie ->
                MovieRow(
                    movie = movie,
                    onClick = { onMovieClick(movie) },
                    onLongClick = onMovieLongClick?.let { longClick -> { longClick(movie) } },
                    modifier = appAnimateItem().testTag("movies_item_${movie.id}"),
                )
                AppDivider(modifier = appAnimateItem())
            }
        }
        item(key = FOOTER_KEY) {
            MoviesLoadFooter(state = movies.loadState.append, onRetry = onRetry)
        }
    }
}

private const val STALE_KEY = "stale"
private const val FOOTER_KEY = "footer"

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    val rows = List(3) { index ->
        Movie(
            id = index,
            title = "Movie ${index + 1}",
            overview = "",
            posterUrl = null,
            releaseDate = LocalDate.of(2000 + index, 1, 1),
            rating = 7.0 + index / 10.0,
        )
    }
    MovieList(
        movies = flowOf(PagingData.from(rows)).collectAsLazyPagingItems(),
        onMovieClick = {},
        onRetry = {},
    )
}
