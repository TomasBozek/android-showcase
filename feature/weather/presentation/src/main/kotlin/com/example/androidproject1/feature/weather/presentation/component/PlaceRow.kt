package com.example.androidproject1.feature.weather.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.SwipeAction
import com.example.androidproject1.core.ui.component.list.SwipeActions
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.core.ui.layout.appSharedElement
import com.example.androidproject1.feature.weather.domain.CurrentConditions
import com.example.androidproject1.feature.weather.domain.WeatherCode
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.feature.weather.presentation.weather.SavedPlace
import com.example.androidproject1.feature.weather.presentation.weather.WeatherState
import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import com.example.androidproject1.service.locale.LocalFormats
import java.time.LocalDateTime

/**
 * One saved place: its name over its region and country, and the cached temperature as the
 * figure on the right — or nothing, when no forecast has been read yet. The temperature is a
 * whole number of degrees: a list is read at a glance, and a tenth of a degree is the detail's
 * to show. Remove is what a swipe reveals, not an X on every row: forty places are forty
 * chances to remove one by accident, and the key carries the id the flow taps.
 *
 * A long press picks the row out, which is how the list enters selection mode; [selected] is what
 * the row looks like once it has been. [swipeable] is off while the screen is selecting — one
 * gesture at a time, so a swipe cannot offer a second way to remove a row mid-choice.
 *
 * @param removeTestTag the revealed key's id — `weather_placeRemoveButton`, the id the X had.
 */
@Composable
fun PlaceRow(
    savedPlace: SavedPlace,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    unit: TemperatureUnit = TemperatureUnit.DEFAULT,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    swipeable: Boolean = true,
    /** `AppReorder`, the handle the row is dragged by; `null` where the list has no order. */
    dragHandle: @Composable (() -> Unit)? = null,
    removeTestTag: String? = null,
) {
    val place = savedPlace.place
    val formats = LocalFormats.current
    AppListItem(
        headline = place.name,
        // The name travels to the forecast's title: a no-op outside the host's transition
        // scope, so no preview and no test sees a difference.
        headlineModifier = Modifier.appSharedElement(placeNameKey(place.id)),
        supporting = listOfNotNull(place.region, place.country).joinToString(", "),
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        modifier = modifier,
        dragHandle = dragHandle,
        swipeActions = if (!swipeable) {
            null
        } else {
            SwipeActions(
                end = SwipeAction(
                    label = stringResource(R.string.weather_remove, place.name),
                    icon = Icons.Filled.Delete,
                    tone = TagTone.Negative,
                    testTag = removeTestTag,
                    onAction = onRemove,
                ),
            )
        },
        trailing = {
            savedPlace.current?.let { current ->
                AppText(
                    text = formats.temperature(current.temperature, unit),
                    role = TextRole.Numeric,
                    modifier = Modifier.testTag("weather_placeValue"),
                )
            }
        },
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    PlaceRow(
        savedPlace = SavedPlace(
            WeatherState.PRAHA,
            CurrentConditions(LocalDateTime.of(2026, 9, 21, 14, 0), 21.3, WeatherCode.Rain, 11.6),
        ),
        onClick = {},
        onRemove = {},
    )
    PlaceRow(savedPlace = SavedPlace(WeatherState.WIEN, null), onClick = {}, onRemove = {})
    PlaceRow(
        savedPlace = SavedPlace(WeatherState.BRNO, null),
        onClick = {},
        onRemove = {},
        selected = true,
        swipeable = false,
    )
}
