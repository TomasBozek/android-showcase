package com.example.androidproject1.feature.weather.presentation.weather

import androidx.lifecycle.SavedStateHandle
import com.example.androidproject1.core.ui.component.choice.CheckState
import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.feature.weather.domain.WeatherRepository
import com.example.androidproject1.feature.weather.domain.WeatherUnitsRepository
import com.example.androidproject1.feature.weather.domain.saveCurrentPlace
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.state.AlertPayload
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.toPluralUiText
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.location.Geocoding
import com.example.androidproject1.service.location.LocationPermissionError
import com.example.androidproject1.service.location.LocationProvider
import com.example.androidproject1.service.location.LocationUnavailableError
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.example.androidproject1.service.core.domain.result.map as mapOutcome

/**
 * What a place is called when the geocoder cannot name it. Asked when it is needed, not
 * held: the name is `My location` in the language the app is in at the moment it is saved. A seam
 * because a view model has no `Context`; `feature/weather/di` binds it over the string resource.
 */
fun interface PlaceNames {

    fun myLocation(): String
}

/**
 * Two reads on one screen: the saved places, joined with what the cache holds for each — the
 * root asks the host for nothing, the detail does that — and the geocoder, debounced from the
 * field and cancelled by the next keystroke.
 */
class WeatherViewModel(
    logger: Logger,
    savedStateHandle: SavedStateHandle,
    private val weatherRepository: WeatherRepository,
    private val weatherUnitsRepository: WeatherUnitsRepository,
    private val locationProvider: LocationProvider,
    private val geocoding: Geocoding,
    private val placeNames: PlaceNames,
) : BaseViewModel<WeatherState, WeatherEvent, WeatherNavigation>(
    initialState = WeatherState(),
    logger = logger.withTag("WeatherViewModel"),
    savedStateHandle = savedStateHandle,
) {

    /** What the remove alert carries back: the places it asked about, not whatever is picked now. */
    private data class RemovePlaces(val placeIds: Set<Int>) : AlertPayload

    /**
     * The places the last remove took away, whole.
     *
     * Whole because undo has to put them back and the rows are gone from the database by then;
     * here rather than in the state because the screen does not draw them, and because the
     * snackbar's action comes back as plain data (`SnackbarAction(id)`) that carries nothing.
     */
    private var lastRemoved: List<Place> = emptyList()

    // Saved, because a half-typed search is exactly the transient state a user notices losing to
    // process death; the flow is what the debounce reads, keystrokes and not every state change.
    private var savedQuery: String by saved(KEY_QUERY, "")
    private val query = MutableStateFlow(savedQuery)

    init {
        if (savedQuery.isNotEmpty()) updateData { copy(query = savedQuery) }
        observePlaces()
        observeUnit()
        observeSuggested()
        observeResults()
    }

    override fun onUiEvent(event: WeatherEvent) {
        when (event) {
            // The field is echoed at once and the search follows 300 ms later: a field that
            // waited for the debounce would drop characters under a fast typist.
            is WeatherEvent.QueryChanged -> {
                updateData { copy(query = event.query) }
                savedQuery = event.query
                query.value = event.query
            }

            is WeatherEvent.ResultClicked -> save(event.place)
            // The same thing a result tapped does: the offer is a shortcut past the typing, not
            // a second way of keeping a place.
            is WeatherEvent.SuggestedPlaceClicked -> save(event.place)
            is WeatherEvent.PlaceClicked -> onPlaceClicked(event.placeId)
            is WeatherEvent.PlaceRemoved -> remove(setOf(event.placeId))
            is WeatherEvent.PlaceMoved -> execute(
                action = { weatherRepository.movePlace(event.placeId, event.toIndex) },
                // No onData work: the saved-places flow is what redraws the list, so writing the
                // new order here as well would be a second source of truth for it.
                onData = {},
            )
            is WeatherEvent.UnitSelected -> execute(
                action = { weatherUnitsRepository.setUnit(event.unit) },
                onData = {},
            )
            is WeatherEvent.PlaceLongPressed -> toggle(event.placeId)
            is WeatherEvent.SelectAllChanged -> updateData {
                // The same rule as the inventory's master: from "some", a tap selects the rest —
                // an indeterminate box reports `false`, and clearing on it would undo the picking
                // the user is halfway through.
                val all = event.checked || selectAllState == CheckState.Indeterminate
                copy(selection = if (all) places.map { it.place.id }.toSet() else emptySet())
            }
            WeatherEvent.RemoveSelectedClicked -> askToRemove()
            WeatherEvent.SelectionCleared -> updateData { copy(selection = emptySet()) }
            WeatherEvent.MyLocationTapped -> useMyLocation()
            // The screen has no Context, so "send them to system settings" is a command like any
            // other and `Screen()` carries it out.
            WeatherEvent.OpenAppSettingsClicked -> sendCommand(UiCommand.OpenAppSettings)
            WeatherEvent.NavigateUpClicked -> navigate(WeatherNavigation.NavigateUp)
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.SnackbarAction && event.id == SNACKBAR_UNDO_REMOVE) {
            undoRemove()
            return
        }
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_REMOVE_SELECTED) {
            uiState.clearAlert()
            (event.payload as? RemovePlaces)?.let { remove(it.placeIds) }
            return
        }
        super.onSystemEvent(event)
    }

    // In selection mode a tap toggles the row, the way the long press started it; otherwise it opens.
    private fun onPlaceClicked(placeId: Int) {
        if (uiState.value.data?.selecting == true) toggle(placeId) else navigate(WeatherNavigation.Forecast(placeId))
    }

    private fun toggle(placeId: Int) = updateData {
        copy(selection = if (placeId in selection) selection - placeId else selection + placeId)
    }

    private fun askToRemove() {
        val ids = uiState.value.data?.selection.orEmpty()
        if (ids.isEmpty()) return
        uiState.setAlert(
            id = ALERT_ID_REMOVE_SELECTED,
            title = R.plurals.weather_remove_selected_title.toPluralUiText(ids.size, ids.size),
            message = R.string.weather_remove_selected_message.toUiText(),
            destructive = true,
            confirmLabel = R.string.weather_remove_selected_confirm.toUiText(),
            declineLabel = R.string.weather_remove_selected_cancel.toUiText(),
            payload = RemovePlaces(ids),
        )
    }

    /**
     * Removes the places and offers them back. The whole `Place` is kept rather than its id: the
     * row is gone from the database, so undo has to re-save what it was, not point at it.
     */
    private fun remove(placeIds: Set<Int>) {
        val state = uiState.value.data ?: return
        lastRemoved = state.places.map { it.place }.filter { it.id in placeIds }
        updateData { copy(selection = selection - placeIds) }
        execute(
            action = {
                placeIds.forEach { weatherRepository.removePlace(it) }
                Outcome.Success(Unit)
            },
            onData = {
                showSnackbar(
                    message = R.plurals.weather_removed.toPluralUiText(placeIds.size, placeIds.size),
                    actionLabel = R.string.weather_removed_undo.toUiText(),
                    id = SNACKBAR_UNDO_REMOVE,
                )
            },
        )
    }

    // No onData work: the observed flow is what redraws the list, so writing it here as well
    // would be a second source of truth for the same rows.
    private fun undoRemove() {
        val places = lastRemoved
        lastRemoved = emptyList()
        if (places.isEmpty()) return
        execute(action = {
            places.forEach { weatherRepository.savePlace(it) }
            Outcome.Success(Unit)
        }, onData = {})
    }

    /**
     * One fix, named where the platform can, saved as the one "my location" place and opened.
     * With no geocoder — or one that knows nothing there — it is saved under [myLocationName] with
     * its coordinates, which is all a forecast needs. A refusal or a device with location off is
     * said in a snackbar: the row stays, and the user can try again.
     */
    private fun useMyLocation() {
        if (uiState.value.data?.locating == true) return
        updateData { copy(locating = true) }
        execute(
            action = {
                when (val fix = locationProvider.current()) {
                    is Outcome.Failure -> fix
                    is Outcome.Success -> {
                        val name = geocoding.name(fix.data) ?: placeNames.myLocation()
                        weatherRepository.saveCurrentPlace(fix.data.latitude, fix.data.longitude, name)
                    }
                }
            },
            onError = { error ->
                updateData { copy(locating = false) }
                if (error is LocationPermissionError || error is LocationUnavailableError) {
                    showSnackbar(R.string.weather_my_location_failed.toUiText())
                    true
                } else {
                    false
                }
            },
            onData = { place ->
                updateData { copy(locating = false) }
                navigate(WeatherNavigation.Forecast(place.id))
            },
        )
    }

    /** Saved first, then opened: the detail reads the forecast by the saved place's coordinates. */
    private fun save(place: Place) = execute(
        action = { weatherRepository.savePlace(place) },
        onData = {
            onUiEvent(WeatherEvent.QueryChanged(""))
            navigate(WeatherNavigation.Forecast(place.id))
        },
    )

    /**
     * The places, each joined with the cache's current conditions. `flatMapLatest` over the
     * list, so a place saved or removed re-joins from scratch; an empty list is a list, not a
     * wait.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observePlaces() = observe(
        flow = {
            weatherRepository.observeSavedPlaces().flatMapLatest { outcome ->
                when (outcome) {
                    is Outcome.Failure -> flowOf(outcome)
                    is Outcome.Success -> joined(outcome.data)
                }
            }
        },
        errorDisplay = ErrorDisplay.Inline,
        onData = { places -> updateData { copy(places = places, loaded = true) } },
    )

    private fun joined(places: List<Place>): Flow<Outcome<List<SavedPlace>>> {
        if (places.isEmpty()) return flowOf(Outcome.Success(emptyList()))
        val each = places.map { place ->
            weatherRepository.observeCachedForecast(place.id).map { outcome ->
                SavedPlace(place, (outcome as? Outcome.Success)?.data?.current)
            }
        }
        return combine(each) { Outcome.Success(it.toList()) }
    }

    // Silent: a preference that cannot be read is a list in Celsius, which is the default and
    // is readable; an alert over the whole screen about it would not be.
    private fun observeUnit() = observe(
        flow = { weatherUnitsRepository.observeUnit() },
        errorDisplay = ErrorDisplay.Silent,
        onData = { unit -> updateData { copy(unit = unit) } },
    )

    // Silent: the offer is a convenience, and a list the user has not asked for cannot be worth
    // an alert over the screen. Its absence is an empty section, which draws as nothing.
    private fun observeSuggested() = observe(
        flow = { weatherRepository.observeSuggestedPlaces() },
        errorDisplay = ErrorDisplay.Silent,
        onData = { places -> updateData { copy(suggested = places) } },
    )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeResults() = observe(
        flow = {
            query
                .debounce(DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .flatMapLatest { text ->
                    if (text.length < WeatherState.MIN_QUERY_LENGTH) {
                        flowOf(Outcome.Success(emptyList()))
                    } else {
                        flow {
                            updateData { copy(searching = true) }
                            emit(weatherRepository.searchPlaces(text).mapOutcome { it })
                        }
                    }
                }
        },
        // A geocoder that is down is a snackbar beside a list that still works, not an error page.
        errorDisplay = ErrorDisplay.Alert,
        onError = {
            updateData { copy(searching = false) }
            false
        },
        onData = { results -> updateData { copy(results = results, searching = false) } },
    )

    companion object {

        /** Its own id, so the undo action is told apart from any other snackbar this screen shows. */
        const val SNACKBAR_UNDO_REMOVE = "weather_undo_remove_place"

        const val ALERT_ID_REMOVE_SELECTED = "weather_remove_selected"

        private const val KEY_QUERY = "query"
        private const val DEBOUNCE_MILLIS = 300L
    }
}
