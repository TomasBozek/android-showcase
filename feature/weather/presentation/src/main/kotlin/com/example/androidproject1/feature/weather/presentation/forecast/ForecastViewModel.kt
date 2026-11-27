package com.example.androidproject1.feature.weather.presentation.forecast

import com.example.androidproject1.feature.weather.api.ForecastDestination
import com.example.androidproject1.feature.weather.domain.Forecast
import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.feature.weather.domain.WeatherRepository
import com.example.androidproject1.feature.weather.domain.WeatherUnitsRepository
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.service.core.domain.error.NotFoundError
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import com.example.androidproject1.service.core.domain.result.map as mapOutcome

/**
 * The place from the saved list and its forecast from the cache-then-network flow, composed
 * into one state; a pull is one round trip behind the indicator, and a failed refresh over a
 * forecast already on screen is a snackbar, not an error page.
 */
class ForecastViewModel(
    logger: Logger,
    // The route key, handed in by the destination. Available in `init`, and it comes back with
    // the back stack entry after process death.
    private val args: ForecastDestination,
    private val weatherRepository: WeatherRepository,
    private val weatherUnitsRepository: WeatherUnitsRepository,
) : BaseViewModel<ForecastState, ForecastEvent, ForecastNavigation>(
    // Nothing to show until the place named by the route and its forecast have loaded.
    initialState = null,
    logger = logger.withTag("ForecastViewModel"),
) {

    private var place: Place? = null
    private var forecast: Forecast? = null
    private var saved = true
    private var lookedUp = false

    init {
        observePlace()
        observeForecast()
        observeUnit()
    }

    // Silent, and applied to whatever is on screen: the forecast is held in Celsius and written
    // in the unit, so a switch redraws the figures without asking the host anything.
    private fun observeUnit() = observe(
        flow = { weatherUnitsRepository.observeUnit() },
        errorDisplay = ErrorDisplay.Silent,
        onData = { unit -> updateData { copy(unit = unit) } },
    )

    override fun onUiEvent(event: ForecastEvent) {
        when (event) {
            is ForecastEvent.TabSelected -> updateData { copy(tab = event.tab) }
            ForecastEvent.Refresh -> refresh()
            ForecastEvent.NavigateUpClicked -> navigate(ForecastNavigation.NavigateUp)
            ForecastEvent.SaveClicked -> save()
        }
    }

    private fun publish() {
        val place = place ?: return
        val forecast = forecast ?: return
        uiState.update { state ->
            state.copy(
                data =
                state.data?.copy(place = place, forecast = forecast, saved = saved)
                    ?: ForecastState(place = place, forecast = forecast, saved = saved),
            )
        }
    }

    // The place is the saved row: a removed place has no forecast to draw, so the read is silent
    // and the screen simply stays on what it had. A place that was never saved — a link's — is
    // looked up once instead.
    private fun observePlace() = observe(
        flow = {
            weatherRepository.observeSavedPlaces().map { outcome ->
                outcome.mapOutcome { list -> list.firstOrNull { it.id == args.placeId } }
            }
        },
        errorDisplay = ErrorDisplay.Silent,
        onData = { found ->
            when {
                found != null -> {
                    place = found
                    saved = true
                    publish()
                }
                place == null && !lookedUp -> lookUp()
            }
        },
    )

    /**
     * The place a link named and nobody saved: shown unsaved, with a Save action. An id nothing
     * knows is said once and the user lands on the places, rather than on an error page.
     */
    private fun lookUp() {
        lookedUp = true
        execute(
            errorDisplay = ErrorDisplay.Silent,
            onError = { error ->
                if (error is NotFoundError) {
                    showToast(R.string.forecast_unknown_place.toUiText())
                    navigate(ForecastNavigation.Weather)
                }
                true
            },
            action = { weatherRepository.findPlace(args.placeId) },
            onData = { found ->
                place = found
                saved = false
                publish()
            },
        )
    }

    private fun save() {
        val place = place ?: return
        execute(
            action = { weatherRepository.savePlace(place) },
            onData = {
                saved = true
                updateData { copy(saved = true) }
            },
        )
    }

    // Cache then network, behind the overlay a cache hit ends on the first frame; inline when
    // nothing is cached, because the screen has nothing else to show.
    private fun observeForecast() = observe(
        flow = { weatherRepository.observeForecast(args.placeId) },
        loading = overlay(),
        errorDisplay = ErrorDisplay.Inline,
        // An id nothing knows fails here as well as in the lookup, which says so and moves on.
        onError = { error -> error is NotFoundError || keepStaleContent() },
        onData = { loaded ->
            forecast = loaded
            publish()
            // A forecast that arrives is a forecast that is no longer stale, whether it came from
            // the cache on first frame or from a refresh that got through.
            updateData { copy(stale = false) }
        },
    )

    /**
     * A usable forecast beats an error page: with one on screen, a failed refresh raises the
     * banner rather than an error state.
     *
     * A banner and not a snackbar (the catalog's rule too): a snackbar says it once
     * and leaves, and the forecast stays old for as long as the person looks at it. The banner
     * carries the time the forecast is from, so "is this this morning's?" is answerable without
     * leaving the screen, and it clears itself the moment a refresh gets through.
     */
    private fun keepStaleContent(): Boolean {
        val hasContent = forecast != null
        if (hasContent) updateData { copy(stale = true) }
        return hasContent
    }

    private fun refresh() {
        if (uiState.value.data?.refreshing == true) return
        execute(
            loading = { refreshing -> updateData { copy(refreshing = refreshing) } },
            action = { weatherRepository.refreshForecast(args.placeId) },
            // The write lands in the cache, and the observation above draws it; nothing to do here.
            onData = { },
        )
    }
}
