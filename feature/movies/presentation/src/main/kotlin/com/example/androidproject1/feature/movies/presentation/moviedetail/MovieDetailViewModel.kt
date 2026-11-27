package com.example.androidproject1.feature.movies.presentation.moviedetail

import com.example.androidproject1.feature.movies.api.MovieDetailDestination
import com.example.androidproject1.feature.movies.domain.MovieDetail
import com.example.androidproject1.feature.movies.domain.MovieFavouritesRepository
import com.example.androidproject1.feature.movies.domain.MoviesRepository
import com.example.androidproject1.feature.movies.presentation.R
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.update

class MovieDetailViewModel(
    logger: Logger,
    // The route key, handed in by the destination. Available in `init`, and it comes back with
    // the back stack entry after process death.
    private val args: MovieDetailDestination,
    private val moviesRepository: MoviesRepository,
    private val movieFavouritesRepository: MovieFavouritesRepository,
) : BaseViewModel<MovieDetailState, MovieDetailEvent, MovieDetailNavigation>(
    // Nothing to show until the movie named by the route has loaded: the id alone draws nothing.
    initialState = null,
    logger = logger.withTag("MovieDetailViewModel"),
) {

    // Two reads — the detail, once, and the heart, for as long as the screen lives — and either
    // can arrive first. Each writes its own half here and the state is composed from the pair,
    // so whichever loses the race is still in the answer (see ProductDetailViewModel).
    private var detail: MovieDetail? = null
    private var isFavourite = false

    init {
        load()
        observeFavourite()
    }

    override fun onUiEvent(event: MovieDetailEvent) = when (event) {
        MovieDetailEvent.FavouriteToggled -> toggleFavourite()
        MovieDetailEvent.PosterClicked -> updateData { copy(viewerOpen = true) }
        is MovieDetailEvent.LinkClicked -> sendCommand(UiCommand.OpenBrowser(event.url))
        MovieDetailEvent.ViewerClosed -> updateData { copy(viewerOpen = false) }
        MovieDetailEvent.NavigateUpClicked -> navigate(MovieDetailNavigation.NavigateUp)
    }

    private fun publish() {
        val detail = detail ?: return
        // A copy of what is there, so a heart arriving does not close the viewer.
        uiState.update {
            it.copy(
                data = it.data?.copy(detail = detail, isFavourite = isFavourite)
                    ?: MovieDetailState(detail = detail, isFavourite = isFavourite),
            )
        }
    }

    // One read, cached or not — the repository decides. Inline: with nothing on screen, an error
    // has nowhere else to go, and the retry it offers re-runs exactly this.
    private fun load() = execute(
        loading = overlay(),
        errorDisplay = ErrorDisplay.Inline,
        action = { moviesRepository.getMovie(args.movieId) },
        onData = { loaded ->
            detail = loaded
            publish()
        },
    )

    /** The heart follows the database, not the tap: one source of truth for one boolean. */
    private fun observeFavourite() = observe(
        flow = { movieFavouritesRepository.observeIsFavourite(args.movieId) },
        errorDisplay = ErrorDisplay.Silent,
        onData = { favourite ->
            isFavourite = favourite
            publish()
        },
    )

    // Alert on failure: the user asked for this, so silence would look like the tap did nothing —
    // and a success says so too, with a way to the list the heart put it on.
    private fun toggleFavourite() {
        val favourite = !isFavourite
        execute(
            action = { movieFavouritesRepository.setFavourite(args.movieId, favourite) },
            onData = {
                showSnackbar(
                    message = if (favourite) {
                        R.string.movie_detail_favourite_added.toUiText()
                    } else {
                        R.string.movie_detail_favourite_removed.toUiText()
                    },
                    actionLabel = R.string.movie_detail_favourite_view.toUiText(),
                    id = SNACKBAR_VIEW_FAVOURITES,
                )
            },
        )
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.SnackbarAction && event.id == SNACKBAR_VIEW_FAVOURITES) {
            navigate(MovieDetailNavigation.OpenFavourites)
            return
        }
        super.onSystemEvent(event)
    }

    private companion object {

        const val SNACKBAR_VIEW_FAVOURITES = "view_favourites"
    }
}
