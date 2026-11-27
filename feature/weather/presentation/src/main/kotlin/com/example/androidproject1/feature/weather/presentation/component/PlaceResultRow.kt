package com.example.androidproject1.feature.weather.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.feature.weather.presentation.weather.WeatherState

/** One geocoder result: the name, and the region and country that tell two Prahas apart. */
@Composable
fun PlaceResultRow(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppListItem(
        headline = place.name,
        supporting = listOfNotNull(place.region, place.country).joinToString(", "),
        onClick = onClick,
        modifier = modifier,
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    PlaceResultRow(place = WeatherState.PRAHA, onClick = {})
    PlaceResultRow(place = WeatherState.WIEN, onClick = {})
}
