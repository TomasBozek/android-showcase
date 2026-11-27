package com.example.androidproject1.feature.movies.presentation.moviefavourites

import com.example.androidproject1.feature.movies.domain.MovieFavouritesRepository
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger

class MovieFavouritesViewModel(
    logger: Logger,
    private val movieFavouritesRepository: MovieFavouritesRepository,
) : BaseViewModel<MovieFavouritesState, MovieFavouritesEvent, MovieFavouritesNavigation>(
    // The shell draws at once; the database answers within a frame, so no overlay.
    initialState = MovieFavouritesState(),
    logger = logger.withTag("MovieFavouritesViewModel"),
) {

    init {
        // For as long as the screen lives: a heart toggled on the detail is reflected here on
        // the way back without a reload.
        observe(
            flow = { movieFavouritesRepository.observeFavourites() },
            errorDisplay = ErrorDisplay.Inline,
            onData = { movies -> updateData { copy(movies = movies, loaded = true) } },
        )
    }

    override fun onUiEvent(event: MovieFavouritesEvent) {
        when (event) {
            is MovieFavouritesEvent.MovieClicked -> navigate(MovieFavouritesNavigation.MovieDetail(event.id))
            is MovieFavouritesEvent.RemoveClicked -> remove(event.id)
            MovieFavouritesEvent.NavigateUpClicked -> navigate(MovieFavouritesNavigation.NavigateUp)
        }
    }

    // No confirm and no undo: the heart on the detail puts it back, and the write is one row.
    private fun remove(id: Int) = execute(
        action = { movieFavouritesRepository.setFavourite(id, false) },
        onData = {},
    )
}
