package com.example.androidproject1.feature.weather.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.status.AppStatusDot
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.weather.domain.WeatherCode
import com.example.androidproject1.feature.weather.presentation.R

/**
 * A [WeatherCode] as a status dot and a word — never an image: the design system has no weather
 * icons, and a coloured dot with a label reads in a row the same way a status does elsewhere.
 * Clear is positive, cloud and fog neutral, rain and drizzle informative, snow and showers a
 * warning, thunder negative.
 */
@Composable
fun WeatherGlyph(
    code: WeatherCode,
    modifier: Modifier = Modifier,
) {
    AppStatusDot(label = stringResource(code.label), tone = code.tone, modifier = modifier)
}

@get:StringRes
val WeatherCode.label: Int
    get() = when (this) {
        WeatherCode.Clear -> R.string.forecast_code_clear
        WeatherCode.Cloudy -> R.string.forecast_code_cloudy
        WeatherCode.Fog -> R.string.forecast_code_fog
        WeatherCode.Drizzle -> R.string.forecast_code_drizzle
        WeatherCode.Rain -> R.string.forecast_code_rain
        WeatherCode.Snow -> R.string.forecast_code_snow
        WeatherCode.Showers -> R.string.forecast_code_showers
        WeatherCode.Thunder -> R.string.forecast_code_thunder
    }

val WeatherCode.tone: TagTone
    get() = when (this) {
        WeatherCode.Clear -> TagTone.Positive
        WeatherCode.Cloudy, WeatherCode.Fog -> TagTone.Neutral
        WeatherCode.Drizzle, WeatherCode.Rain -> TagTone.Info
        WeatherCode.Snow, WeatherCode.Showers -> TagTone.Warning
        WeatherCode.Thunder -> TagTone.Negative
    }

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    WeatherCode.entries.forEach { WeatherGlyph(code = it, modifier = Modifier.padding(AppTheme.spacing.stack.xs)) }
}
