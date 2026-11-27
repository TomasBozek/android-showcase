package com.example.androidproject1.feature.movies.presentation.movies

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.feature.movies.domain.MovieFavouritesRepository
import com.example.androidproject1.feature.movies.domain.MoviesCache
import com.example.androidproject1.feature.movies.domain.MoviesRepository
import com.example.androidproject1.feature.movies.presentation.R
import com.example.androidproject1.service.core.domain.lifecycle.Foreground
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toPluralUiText
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * The list is Paging's: [movies] is the repository's pager, cached in this view model's
 * scope so a rotation does not start it again, and the screen collects it. Everything a list used
 * to keep here — which page is next, whether the end is reached, what a refresh throws away, the
 * stale banner — is a load state on the items now, so what is left is the long-pressed row.
 */
class MoviesViewModel(
    logger: Logger,
    moviesRepository: MoviesRepository,
    private val movieFavouritesRepository: MovieFavouritesRepository,
    foreground: Foreground,
    private val clock: Clock,
) : BaseViewModel<MoviesState, MoviesEvent, MoviesNavigation>(
    initialState = MoviesState(),
    logger = logger.withTag("MoviesViewModel"),
) {

    /** The rows. Collected by the destination with `collectAsLazyPagingItems`. */
    val movies: Flow<PagingData<Movie>> = moviesRepository.pagedMovies().cachedIn(viewModelScope)

    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)

    /**
     * Each one is a refresh the destination answers on the paging items: the app came back
     * after being away longer than the cache may be old, so the list on screen may be too.
     */
    val refreshes: Flow<Unit> = refreshRequests.receiveAsFlow()

    /** When the rows on screen were fetched, for the failed refresh to say how old they are. */
    private var lastUpdated: Instant? = null

    init {
        observe(flow = { moviesRepository.observeLastUpdated().map { Outcome.Success(it) } }) { at ->
            lastUpdated = at
            val local = at?.atZone(clock.zone)?.toLocalDateTime()
            updateData {
                copy(lastUpdated = local, lastUpdatedToday = local?.toLocalDate() == LocalDate.now(clock))
            }
        }

        var leftAt: Long? = null
        foreground.isForeground
            .onEach { inFront ->
                if (!inFront) {
                    leftAt = clock.millis()
                } else {
                    val away = leftAt?.let { clock.millis() - it }
                    leftAt = null
                    if (away != null && away >= MoviesCache.MAX_AGE_MILLIS) refreshRequests.trySend(Unit)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onUiEvent(event: MoviesEvent) {
        when (event) {
            // The paging items own both, so the destination — which holds them — answers them
            // before the event gets here; a view model has no way to call `refresh()` on a
            // `LazyPagingItems`, and holding a second copy of the list to do so would be the
            // bookkeeping this replaced.
            MoviesEvent.Refresh, MoviesEvent.Retry -> Unit
            MoviesEvent.RefreshFailed -> lastUpdated?.let { showSnackbar(ageMessage(it)) }
            is MoviesEvent.MovieClicked -> navigate(MoviesNavigation.MovieDetail(event.id))
            is MoviesEvent.MovieLongPressed -> updateData { copy(actionMovie = event.movie) }
            MoviesEvent.ActionSheetDismissed -> updateData { copy(actionMovie = null) }
            MoviesEvent.FavouriteFromSheetClicked -> favouriteFromSheet()
            MoviesEvent.FavouritesClicked -> navigate(MoviesNavigation.Favourites)
            MoviesEvent.NavigateUpClicked -> navigate(MoviesNavigation.NavigateUp)
        }
    }

    /** "Could not refresh. These are from 25 minutes ago." — minutes, then hours, then days. */
    private fun ageMessage(at: Instant): UiText {
        val minutes = Duration.between(at, clock.instant()).toMinutes().coerceAtLeast(0)
        return when {
            minutes < MINUTES_PER_HOUR -> R.plurals.movies_refresh_failed_minutes.toPluralUiText(
                minutes.toInt(),
                minutes.toInt(),
            )
            minutes < MINUTES_PER_DAY -> (minutes / MINUTES_PER_HOUR).toInt().let {
                R.plurals.movies_refresh_failed_hours.toPluralUiText(it, it)
            }
            else -> (minutes / MINUTES_PER_DAY).toInt().let {
                R.plurals.movies_refresh_failed_days.toPluralUiText(it, it)
            }
        }
    }

    /** The heart from the sheet: writes the favourite and says so, since the row shows no heart of its own. */
    private fun favouriteFromSheet() {
        val movie = uiState.value.data?.actionMovie ?: return
        updateData { copy(actionMovie = null) }
        execute(
            action = { movieFavouritesRepository.setFavourite(movie.id, true) },
            onData = { showSnackbar(R.string.movies_favourite_added.toUiText()) },
        )
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60L
        const val MINUTES_PER_DAY = 24 * 60L
    }
}
