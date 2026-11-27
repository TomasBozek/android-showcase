package com.example.androidproject1.feature.movies.presentation.moviedetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.content.AppHtmlText
import com.example.androidproject1.core.ui.component.content.AppImage
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppDescriptionList
import com.example.androidproject1.core.ui.component.list.DescriptionRow
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.overlay.AppImageViewer
import com.example.androidproject1.core.ui.layout.appSharedElement
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.movies.presentation.R
import com.example.androidproject1.feature.movies.presentation.component.MovieGenres
import com.example.androidproject1.feature.movies.presentation.component.moviePosterKey
import com.example.androidproject1.service.locale.LocalFormats

@Composable
fun MovieDetailScreen(
    state: MovieDetailState,
    onEvent: (MovieDetailEvent) -> Unit,
) {
    val detail = state.detail
    val movie = detail.movie
    val formats = LocalFormats.current
    AppScaffold(
        screenId = "MovieDetailScreen",
        topBar = {
            AppTopBar(
                title = movie.title,
                onNavigateUp = { onEvent(MovieDetailEvent.NavigateUpClicked) },
                navigateUpTestTag = "movieDetail_upButton",
                actions = {
                    // The icon carries the same string as its contentDescription, so the screen
                    // reader and the test reach it the same way — and the label says what the tap
                    // will do, not what the state is.
                    val favouriteLabel = stringResource(
                        if (state.isFavourite) {
                            R.string.movie_detail_favourite_remove
                        } else {
                            R.string.movie_detail_favourite_add
                        },
                    )
                    AppIconButton(
                        icon = if (state.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = favouriteLabel,
                        onClick = { onEvent(MovieDetailEvent.FavouriteToggled) },
                        modifier = Modifier.testTag("movieDetail_favouriteButton"),
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.lg)) {
                // Arrives from the row that opened this screen; see MovieRow.
                // A tap opens it full screen; the name is on the tappable node, and says what
                // the tap does.
                val openPoster = stringResource(R.string.movie_detail_poster_open)
                AppImage(
                    model = movie.posterUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .width(AppTheme.density.listRowHeight * POSTER_ROWS)
                        .aspectRatio(POSTER_RATIO)
                        .appSharedElement(moviePosterKey(movie.id))
                        .clickable { onEvent(MovieDetailEvent.PosterClicked) }
                        .semantics { contentDescription = openPoster }
                        .testTag("movieDetail_posterTile"),
                )
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                    detail.tagline?.let { AppText(text = it, role = TextRole.BodyLarge) }
                    AppDescriptionList(
                        rows = listOf(
                            DescriptionRow(
                                label = stringResource(R.string.movie_detail_released),
                                value = movie.releaseDate?.let(formats::date),
                            ),
                            DescriptionRow(
                                label = stringResource(R.string.movie_detail_runtime),
                                value = detail.runtime?.let(formats::duration),
                            ),
                            DescriptionRow(
                                label = stringResource(R.string.movie_detail_rating),
                                value = formats.percent(movie.rating / RATING_SCALE),
                                numeric = true,
                            ),
                        ),
                        modifier = Modifier.testTag("movieDetail_factsList"),
                    )
                }
            }
            MovieGenres(
                genres = detail.genres,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("movieDetail_genreGroup"),
            )
            if (movie.overview.isNotBlank()) {
                // The overview is the server's text, which a CMS sends as HTML.
                AppHtmlText(
                    html = movie.overview,
                    role = TextRole.Body,
                    onLink = { onEvent(MovieDetailEvent.LinkClicked(it)) },
                    modifier = Modifier.testTag("movieDetail_overviewValue"),
                )
            }
        }
        if (state.viewerOpen) {
            AppImageViewer(
                images = listOf(movie.posterUrl),
                onClose = { onEvent(MovieDetailEvent.ViewerClosed) },
                contentDescription = movie.title,
            )
        }
    }
}

private const val RATING_SCALE = 10.0
private const val POSTER_RATIO = 2f / 3f

/** The poster is two list rows wide: big enough to be the picture, small enough to leave the facts room. */
private const val POSTER_ROWS = 2

@ScreenPreview
@Composable
private fun Preview(
    @PreviewParameter(MovieDetailStatePreviews::class) state: MovieDetailState,
) = ThemedScreenPreview {
    MovieDetailScreen(
        state = state,
    ) {}
}
