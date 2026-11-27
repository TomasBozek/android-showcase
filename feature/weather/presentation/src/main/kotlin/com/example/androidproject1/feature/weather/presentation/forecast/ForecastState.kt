package com.example.androidproject1.feature.weather.presentation.forecast

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.androidproject1.feature.weather.domain.CurrentConditions
import com.example.androidproject1.feature.weather.domain.DailyEntry
import com.example.androidproject1.feature.weather.domain.Forecast
import com.example.androidproject1.feature.weather.domain.HourlyEntry
import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.feature.weather.domain.WeatherCode
import com.example.androidproject1.feature.weather.presentation.weather.WeatherState
import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import java.time.LocalDate
import java.time.LocalDateTime

/** The place, its forecast, which tab is up, and whether a pull is in flight. */
@Immutable
data class ForecastState(
    val place: Place,
    val forecast: Forecast,
    val tab: ForecastTab = ForecastTab.Today,
    val refreshing: Boolean = false,
    /** The scale every temperature on this screen is written in; the forecast is Celsius. */
    val unit: TemperatureUnit = TemperatureUnit.DEFAULT,
    /**
     * A refresh failed over a forecast already on screen.
     *
     * It is a banner above the list rather than a snackbar, which is the rule the catalog
     * follows too: a snackbar says it once and leaves, and what is on screen stays old for as long as
     * the person looks at it. The banner carries the time it is showing, so "from 14:05" is
     * answerable without leaving the screen.
     */
    val stale: Boolean = false,
    /**
     * Whether the place is on the saved list. A link can name a place nobody saved: its
     * forecast is shown all the same, with a Save action until it is kept.
     */
    val saved: Boolean = true,
) {

    /** The next twenty-four hours as a line: what the Today tab draws above its rows. */
    val todayTemperatures: List<Float>
        get() = today.map { it.temperature.toFloat() }

    /** The same hours' rain probability, 0–1, drawn as columns under the line. */
    val todayRain: List<Float>
        get() = today.map { it.precipitationProbability.toFloat() }

    /** The current hour's place in [today], so the line can mark where now is. */
    val currentHourIndex: Int
        get() = 0

    /** The week's highs and lows, the two lines the Week tab draws over its rows. */
    val weekHighs: List<Float>
        get() = forecast.daily.map { it.temperatureMax.toFloat() }

    val weekLows: List<Float>
        get() = forecast.daily.map { it.temperatureMin.toFloat() }

    /** The next twenty-four hours from the current hour, not the day's midnight: what is coming. */
    val today: List<HourlyEntry>
        get() {
            val from = forecast.current.time.withMinute(0).withSecond(0).withNano(0)
            return forecast.hourly.filter { !it.time.isBefore(from) }.take(HOURS)
        }

    companion object {

        const val HOURS = 24

        val PREVIEW = ForecastState(
            place = WeatherState.PRAHA,
            forecast = Forecast(
                current = CurrentConditions(LocalDateTime.of(2026, 9, 21, 14, 15), 21.3, WeatherCode.Rain, 11.6),
                hourly = List(48) { hour ->
                    HourlyEntry(
                        time = LocalDateTime.of(2026, 9, 21, 0, 0).plusHours(hour.toLong()),
                        temperature = 12.0 + (hour % 24) * 0.5,
                        precipitationProbability = (hour % 10) / 10.0,
                        code = WeatherCode.entries[hour % WeatherCode.entries.size],
                    )
                },
                fetchedAt = LocalDateTime.of(2026, 9, 21, 14, 5),
                daily = List(7) { day ->
                    DailyEntry(
                        date = LocalDate.of(2026, 9, 21).plusDays(day.toLong()),
                        code = WeatherCode.entries[day % WeatherCode.entries.size],
                        temperatureMax = 18.0 + day,
                        temperatureMin = 9.0 + day * 0.5,
                        precipitation = day * 1.5,
                    )
                },
            ),
        )
    }
}

enum class ForecastTab { Today, Week }

/** The states this screen is drawn in: today, the week, a pull in flight, and a place not saved. */
class ForecastStatePreviews : PreviewParameterProvider<ForecastState> {

    override val values = sequenceOf(
        ForecastState.PREVIEW,
        ForecastState.PREVIEW.copy(tab = ForecastTab.Week),
        ForecastState.PREVIEW.copy(refreshing = true),
        ForecastState.PREVIEW.copy(saved = false),
    )
}
