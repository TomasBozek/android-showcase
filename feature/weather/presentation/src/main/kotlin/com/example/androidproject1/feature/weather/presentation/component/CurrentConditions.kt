package com.example.androidproject1.feature.weather.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppCounter
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppCard
import com.example.androidproject1.feature.weather.domain.CurrentConditions
import com.example.androidproject1.feature.weather.domain.WeatherCode
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import com.example.androidproject1.service.locale.LocalFormats
import java.time.LocalDateTime

/** Now: the temperature as the one big figure, the condition under it, the wind and the time beside. */
@Composable
fun CurrentConditions(
    current: CurrentConditions,
    unit: TemperatureUnit = TemperatureUnit.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val formats = LocalFormats.current
    AppCard(modifier = modifier.fillMaxWidth()) {
        // The temperature rolls on a refresh.
        AppCounter(
            text = formats.temperature(current.temperature, unit),
            role = TextRole.DisplayLarge,
        )
        WeatherGlyph(code = current.code)
        AppText(
            text = stringResource(
                R.string.forecast_wind_at,
                formats.quantity(current.windSpeed.toLong()),
                formats.time(current.time.toLocalTime()),
            ),
            role = TextRole.Secondary,
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    CurrentConditions(current = CurrentConditions(LocalDateTime.of(2026, 9, 21, 14, 15), 21.3, WeatherCode.Rain, 11.6))
}
