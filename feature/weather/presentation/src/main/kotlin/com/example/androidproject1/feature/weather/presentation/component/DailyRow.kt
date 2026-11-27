package com.example.androidproject1.feature.weather.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.feature.weather.domain.DailyEntry
import com.example.androidproject1.feature.weather.domain.WeatherCode
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import com.example.androidproject1.service.locale.LocalFormats
import java.time.LocalDate

/** One day: the date, the condition, the precipitation, and the high over the low as the figure. */
@Composable
fun DailyRow(
    entry: DailyEntry,
    unit: TemperatureUnit = TemperatureUnit.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val formats = LocalFormats.current
    AppListItem(
        headline = formats.date(entry.date),
        supporting = stringResource(R.string.forecast_precipitation, formats.quantity(entry.precipitation.toLong())),
        modifier = modifier,
        leading = { WeatherGlyph(code = entry.code) },
        trailing = {
            AppText(
                // The two figures in one run, joined by the separator rather than a format
                // string: each is a `temperature` role, so each carries its own unit symbol.
                text = "${formats.temperature(entry.temperatureMax, unit)}$HIGH_LOW_SEPARATOR" +
                    formats.temperature(entry.temperatureMin, unit),
                role = TextRole.Numeric,
            )
        },
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    DailyRow(entry = DailyEntry(LocalDate.of(2026, 9, 21), WeatherCode.Rain, 22.0, 11.5, 1.2))
    DailyRow(entry = DailyEntry(LocalDate.of(2026, 9, 22), WeatherCode.Clear, 24.0, 12.0, 0.0))
}

/** Between a day's high and its low. A thin space either side, the way a range is written. */
private const val HIGH_LOW_SEPARATOR = " / "
