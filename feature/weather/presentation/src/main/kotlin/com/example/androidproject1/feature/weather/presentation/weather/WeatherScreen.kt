package com.example.androidproject1.feature.weather.presentation.weather

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.action.IconButtonKind
import com.example.androidproject1.core.ui.component.choice.AppCheckbox
import com.example.androidproject1.core.ui.component.choice.AppSegmented
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.content.AppReveal
import com.example.androidproject1.core.ui.component.content.AppSwitcher
import com.example.androidproject1.core.ui.component.field.AppSearchField
import com.example.androidproject1.core.ui.component.list.AppReorder
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppToolbar
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.status.AppIndeterminateProgress
import com.example.androidproject1.core.ui.component.status.AppSkeleton
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.core.ui.layout.appReorderItem
import com.example.androidproject1.core.ui.layout.rememberReorderState
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.feature.weather.presentation.component.MyLocationRow
import com.example.androidproject1.feature.weather.presentation.component.PlaceResultRow
import com.example.androidproject1.feature.weather.presentation.component.PlaceRow
import com.example.androidproject1.service.core.domain.format.TemperatureUnit

@Composable
fun WeatherScreen(
    state: WeatherState,
    onEvent: (WeatherEvent) -> Unit,
) {
    AppScaffold(
        screenId = "WeatherScreen",
        topBar = {
            // Selection mode covers the bar rather than replacing it: the toolbar slides
            // down over the top bar and back up, so the bar underneath never jumps, and the Box
            // is as tall as the taller of the two so nothing below moves either way.
            Box {
                AppTopBar(
                    title = stringResource(R.string.weather_title),
                    onNavigateUp = { onEvent(WeatherEvent.NavigateUpClicked) },
                    navigateUpTestTag = "weather_upButton",
                )
                AppReveal(visible = state.selecting, slide = true, modifier = Modifier.matchParentSize()) {
                    AppToolbar(
                        title = stringResource(R.string.weather_selected_count, state.selection.size),
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("weather_selectionGroup"),
                    ) {
                        AppCheckbox(
                            checked = state.selectAllState,
                            onCheckedChange = { onEvent(WeatherEvent.SelectAllChanged(it)) },
                            label = stringResource(R.string.weather_select_all),
                            inverse = true,
                            modifier = Modifier.testTag("weather_selectAllCheckbox"),
                        )
                        AppIconButton(
                            icon = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.weather_remove_selected),
                            onClick = { onEvent(WeatherEvent.RemoveSelectedClicked) },
                            kind = IconButtonKind.Inverse,
                            modifier = Modifier.testTag("weather_removeSelectedButton"),
                        )
                        AppIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.weather_selection_clear),
                            onClick = { onEvent(WeatherEvent.SelectionCleared) },
                            kind = IconButtonKind.Inverse,
                            modifier = Modifier.testTag("weather_selectionCloseButton"),
                        )
                    }
                }
            }
        },
        contentPadding = false,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Above the search: where the user is is the answer most people want first.
            MyLocationRow(
                locating = state.locating,
                onUse = { onEvent(WeatherEvent.MyLocationTapped) },
                onOpenSettings = { onEvent(WeatherEvent.OpenAppSettingsClicked) },
                modifier = Modifier.padding(
                    start = AppTheme.spacing.inset.md,
                    end = AppTheme.spacing.inset.md,
                    top = AppTheme.spacing.inset.md,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.inset.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
            ) {
                AppSearchField(
                    value = state.query,
                    onValueChange = { onEvent(WeatherEvent.QueryChanged(it)) },
                    placeholder = stringResource(R.string.weather_search_placeholder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weather_queryField"),
                )
                // Beside the field rather than in Settings: the unit is what this screen's
                // figures are in, and it is switched while looking at them.
                val units = TemperatureUnit.entries
                AppSegmented(
                    options = units.map { stringResource(it.labelRes()) },
                    selectedIndex = units.indexOf(state.unit),
                    onSelect = { onEvent(WeatherEvent.UnitSelected(units[it])) },
                    // A touch target per option, from the density's own floor: `AppSegmented`
                    // sizes itself off the constraint it is given, so an unbounded one takes the
                    // whole row and a share of it makes each option too small to hit.
                    modifier = Modifier
                        .width(AppTheme.density.minTouchTarget * units.size)
                        .testTag("weather_unitTab"),
                )
            }
            // Under the field and over the results: a search that has been asked and has not
            // answered is the one thing the user is waiting for, and it belongs where they are
            // already looking.
            AppReveal(visible = state.searching) {
                AppIndeterminateProgress(
                    label = stringResource(R.string.weather_searching),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.inset.md)
                        .testTag("weather_searchProgress"),
                )
            }
            // The results replace the list while a search is on: the field is the one thing the
            // user is looking at, and what it found belongs under it. The four cross-fade:
            // they are not in an order, so nothing slides.
            val pane = when {
                state.noResults -> PANE_NO_RESULTS
                state.showResults -> PANE_RESULTS
                !state.loaded -> PANE_LOADING
                state.places.isEmpty() -> PANE_EMPTY
                else -> PANE_PLACES
            }
            AppSwitcher(target = pane, slide = false, modifier = Modifier.fillMaxSize()) { shown ->
                when (shown) {
                    PANE_RESULTS -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("weather_resultList"),
                    ) {
                        items(state.results, key = { it.id }) { place ->
                            PlaceResultRow(
                                place = place,
                                onClick = { onEvent(WeatherEvent.ResultClicked(place)) },
                                modifier = appAnimateItem().testTag("weather_resultItem"),
                            )
                            AppDivider(modifier = appAnimateItem())
                        }
                    }

                    // A search that answered nothing, rather than the blank the results pane used
                    // to leave: the query is quoted back, because the usual cause is a typo.
                    PANE_NO_RESULTS -> AppEmptyState(
                        title = stringResource(R.string.weather_results_empty_title),
                        message = stringResource(R.string.weather_results_empty_message, state.query),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("weather_resultsEmpty"),
                    )

                    PANE_LOADING -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(SKELETON_ROWS) {
                            AppSkeleton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppTheme.spacing.inset.lg)
                                    .testTag("weather_placeSkeleton"),
                            )
                        }
                    }

                    // Nothing saved: the empty state says so and then gets out of the way, so
                    // the offer below it is what a first open actually has to tap. It no
                    // longer fills the screen, which is what left the first open with nothing.
                    PANE_EMPTY -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("weather_placeList"),
                    ) {
                        item {
                            AppEmptyState(
                                title = stringResource(R.string.weather_empty_title),
                                message = stringResource(R.string.weather_empty_message),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("weather_empty"),
                            )
                        }
                        suggested(state, onEvent)
                    }

                    else -> {
                        // The list's own scroll state belongs to the reorder, so the drag can
                        // read where the rows actually are.
                        val reorder = rememberReorderState { from, to ->
                            state.places.getOrNull(from)?.let {
                                onEvent(WeatherEvent.PlaceMoved(it.place.id, to))
                            }
                        }
                        LazyColumn(
                            state = reorder.listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("weather_placeList"),
                        ) {
                            items(state.places, key = { it.place.id }) { saved ->
                                PlaceRow(
                                    savedPlace = saved,
                                    onClick = { onEvent(WeatherEvent.PlaceClicked(saved.place.id)) },
                                    onRemove = { onEvent(WeatherEvent.PlaceRemoved(saved.place.id)) },
                                    onLongClick = { onEvent(WeatherEvent.PlaceLongPressed(saved.place.id)) },
                                    selected = saved.place.id in state.selection,
                                    // One gesture at a time: a swipe while rows are being picked
                                    // would offer a second way to remove one, on a row mid-choice.
                                    swipeable = !state.selecting,
                                    unit = state.unit,
                                    // Nothing to drag while picking rows: the toolbar is a mode.
                                    dragHandle = if (state.selecting) {
                                        null
                                    } else {
                                        { AppReorder(state = reorder, key = saved.place.id) }
                                    },
                                    modifier = appReorderItem(reorder, saved.place.id)
                                        .testTag("weather_placeItem"),
                                    removeTestTag = "weather_placeRemoveButton",
                                )
                                AppDivider(modifier = appAnimateItem())
                            }
                            suggested(state, onEvent)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The places on offer, at the end of whichever list is showing: a header and the rows
 * that are not saved yet. Nothing while rows are being picked — the toolbar is a mode, and an
 * offer to add one more inside it is an invitation to tap the wrong thing.
 */
private fun LazyListScope.suggested(state: WeatherState, onEvent: (WeatherEvent) -> Unit) {
    if (state.suggested.isEmpty() || state.selecting) return
    item {
        AppSectionHeader(
            title = stringResource(R.string.weather_suggested_title),
            // The rows carry their inset inside `AppListItem`; a header does not, so the caller
            // supplies it — otherwise the label sits at the screen edge and the rows do not.
            modifier = appAnimateItem().padding(horizontal = AppTheme.spacing.inset.lg),
        )
    }
    items(state.suggested, key = { "suggested_${it.id}" }) { place ->
        PlaceResultRow(
            place = place,
            onClick = { onEvent(WeatherEvent.SuggestedPlaceClicked(place)) },
            modifier = appAnimateItem().testTag("weather_suggestedItem"),
        )
        AppDivider(modifier = appAnimateItem())
    }
}

/** The label for a unit, so the control shows `°C | °F` and the strings stay translatable. */
@StringRes
private fun TemperatureUnit.labelRes(): Int = when (this) {
    TemperatureUnit.Celsius -> R.string.weather_unit_celsius
    TemperatureUnit.Fahrenheit -> R.string.weather_unit_fahrenheit
}

/** The things the screen shows under the field, as an index for the switcher. */
private const val PANE_RESULTS = 0
private const val PANE_NO_RESULTS = 1
private const val PANE_LOADING = 2
private const val PANE_EMPTY = 3
private const val PANE_PLACES = 4

/** Enough skeleton rows to read as a list rather than as one thing loading. */
private const val SKELETON_ROWS = 3

@ScreenPreview
@Composable
private fun Preview(
    @PreviewParameter(WeatherStatePreviews::class) state: WeatherState,
) = ThemedScreenPreview {
    WeatherScreen(
        state = state,
    ) {}
}
