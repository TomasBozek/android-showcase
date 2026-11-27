package com.example.androidproject1.feature.weather.presentation.weather

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.androidproject1.core.ui.component.choice.CheckState
import com.example.androidproject1.feature.weather.domain.CurrentConditions
import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.feature.weather.domain.WeatherCode
import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import java.time.LocalDateTime

/**
 * The root: what the user typed, what the geocoder found for it, and the places they kept —
 * each with the current conditions the cache holds for it, or none.
 */
@Immutable
data class WeatherState(
    val query: String = "",
    val results: List<Place> = emptyList(),
    val searching: Boolean = false,
    val places: List<SavedPlace> = emptyList(),
    /** The saved places have answered once, so an empty list means empty rather than not yet read. */
    val loaded: Boolean = false,
    /** The ids a long press picked out; empty means the screen is not in selection mode. */
    val selection: Set<Int> = emptySet(),
    /** Places to pick from before anything is typed: the bundled list less what is saved. */
    val suggested: List<Place> = emptyList(),
    /** A fix has been asked for and the place is not saved yet. */
    val locating: Boolean = false,
    /** The scale every temperature on this screen is written in. */
    val unit: TemperatureUnit = TemperatureUnit.DEFAULT,
) {

    val showResults: Boolean get() = query.length >= MIN_QUERY_LENGTH

    /** A search that has answered and found nothing — not a search that has not answered yet. */
    val noResults: Boolean get() = showResults && !searching && results.isEmpty()

    val selecting: Boolean get() = selection.isNotEmpty()

    /** Off, on, or the in-between the toolbar's checkbox draws when some rows are picked. */
    val selectAllState: CheckState
        get() = when {
            selection.isEmpty() -> CheckState.Off
            places.all { it.place.id in selection } -> CheckState.On
            else -> CheckState.Indeterminate
        }

    companion object {

        /** Under two characters the geocoder answers half the world; a search waits for the second. */
        const val MIN_QUERY_LENGTH = 2

        val PRAHA = Place(3067696, "Praha", "Česko", "Praha", 50.088, 14.421, "Europe/Prague")
        val BRNO = Place(3078610, "Brno", "Česko", "Jihomoravský kraj", 49.195, 16.608, "Europe/Prague")
        val WIEN = Place(2761369, "Wien", "Österreich", null, 48.208, 16.372, "Europe/Vienna")
        val OSTRAVA = Place(3068799, "Ostrava", "Česko", "Moravskoslezský kraj", 49.835, 18.292, "Europe/Prague")
        val PLZEN = Place(3068722, "Plzeň", "Česko", "Plzeňský kraj", 49.747, 13.377, "Europe/Prague")
        val BERLIN = Place(2950159, "Berlin", "Deutschland", "Berlin", 52.524, 13.410, "Europe/Berlin")

        val PREVIEW = WeatherState(
            places = listOf(
                SavedPlace(
                    PRAHA,
                    CurrentConditions(LocalDateTime.of(2026, 9, 21, 14, 0), 21.3, WeatherCode.Rain, 11.6),
                ),
                SavedPlace(BRNO, CurrentConditions(LocalDateTime.of(2026, 9, 21, 14, 0), 19.0, WeatherCode.Clear, 7.2)),
                SavedPlace(WIEN, null),
            ),
            loaded = true,
            suggested = listOf(OSTRAVA, PLZEN, BERLIN),
        )
    }
}

/** A saved place and the conditions the cache holds for it — `null` until its forecast was fetched once. */
@Immutable
data class SavedPlace(
    val place: Place,
    val current: CurrentConditions?,
)

/** The states this screen is drawn in: the list, a search in progress, and nothing saved yet. */
class WeatherStatePreviews : PreviewParameterProvider<WeatherState> {

    override val values = sequenceOf(
        WeatherState.PREVIEW,
        WeatherState.PREVIEW.copy(
            query = "Pra",
            results = listOf(
                WeatherState.PRAHA,
                WeatherState.PRAHA.copy(id = 1, country = "Slovensko", region = "Banskobystrický kraj"),
            ),
        ),
        WeatherState(loaded = true, suggested = listOf(WeatherState.PRAHA, WeatherState.BRNO, WeatherState.WIEN)),
        WeatherState.PREVIEW.copy(selection = setOf(WeatherState.PRAHA.id)),
        WeatherState(query = "zzz", loaded = true),
    )
}
