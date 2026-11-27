package com.example.androidproject1.feature.weather.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.feature.weather.domain.HourlyEntry
import com.example.androidproject1.feature.weather.domain.WeatherCode
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import com.example.androidproject1.service.locale.LocalFormats
import java.time.LocalDateTime

/** One hour: the time, the condition, the chance of rain as a percentage, the temperature as the figure. */
@Composable
fun HourlyRow(
    entry: HourlyEntry,
    unit: TemperatureUnit = TemperatureUnit.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val formats = LocalFormats.current
    AppListItem(
        headline = formats.time(entry.time.toLocalTime()),
        supporting = stringResource(R.string.forecast_rain_chance, formats.percent(entry.precipitationProbability)),
        modifier = modifier,
        leading = { WeatherGlyph(code = entry.code) },
        trailing = {
            AppText(
                text = formats.temperature(entry.temperature, unit),
                role = TextRole.Numeric,
            )
        },
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    HourlyRow(entry = HourlyEntry(LocalDateTime.of(2026, 9, 21, 15, 0), 21.3, 0.4, WeatherCode.Rain))
    HourlyRow(entry = HourlyEntry(LocalDateTime.of(2026, 9, 21, 16, 0), 20.1, 0.0, WeatherCode.Clear))
}
