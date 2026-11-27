package com.example.androidproject1.feature.weather.presentation.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.content.AppSparkline
import com.example.androidproject1.core.ui.component.content.AppSwitcher
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppPullToRefresh
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTabs
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.navigation.TabItem
import com.example.androidproject1.core.ui.component.status.AppBanner
import com.example.androidproject1.core.ui.component.status.BannerTone
import com.example.androidproject1.core.ui.layout.appSharedElement
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.feature.weather.presentation.component.CurrentConditions
import com.example.androidproject1.feature.weather.presentation.component.DailyRow
import com.example.androidproject1.feature.weather.presentation.component.HourlyRow
import com.example.androidproject1.feature.weather.presentation.component.placeNameKey
import com.example.androidproject1.service.locale.LocalFormats

@Composable
fun ForecastScreen(
    state: ForecastState,
    onEvent: (ForecastEvent) -> Unit,
) {
    AppScaffold(
        screenId = "ForecastScreen",
        topBar = {
            AppTopBar(
                title = state.place.name,
                // The other half of the row's headline: the name a finger landed on is the
                // name that ends up here, rather than one fading out as another fades in.
                titleModifier = Modifier.appSharedElement(placeNameKey(state.place.id)),
                onNavigateUp = { onEvent(ForecastEvent.NavigateUpClicked) },
                navigateUpTestTag = "forecast_upButton",
                actions = {
                    // A link can open a place nobody saved: its forecast is shown, and this
                    // keeps it.
                    if (!state.saved) {
                        AppButton(
                            label = stringResource(R.string.forecast_save),
                            onClick = { onEvent(ForecastEvent.SaveClicked) },
                            kind = ButtonKind.Ghost,
                            modifier = Modifier.testTag("forecast_saveButton"),
                        )
                    }
                },
            )
        },
        contentPadding = false,
    ) {
        AppPullToRefresh(
            refreshing = state.refreshing,
            onRefresh = { onEvent(ForecastEvent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            // One list for both tabs, so the pull reaches the whole screen and the header scrolls
            // away with the rows rather than pinning them under it.
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item(key = "stale") {
                    // A banner above the list, not a snackbar: a snackbar says it once and leaves,
                    // and what is on screen stays old for as long as the person looks at it.
                    AppBanner(
                        text = stringResource(
                            R.string.forecast_stale,
                            LocalFormats.current.time(state.forecast.fetchedAt.toLocalTime()),
                        ),
                        tone = BannerTone.Info,
                        visible = state.stale,
                        modifier = Modifier.testTag("forecast_staleBadge"),
                    )
                }
                item(key = "current") {
                    Column(
                        modifier = Modifier.padding(AppTheme.spacing.inset.md),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
                    ) {
                        CurrentConditions(
                            current = state.forecast.current,
                            unit = state.unit,
                            modifier = Modifier.testTag("forecast_currentCard"),
                        )
                        AppText(
                            text = stringResource(
                                R.string.forecast_updated_at,
                                LocalFormats.current.time(state.forecast.fetchedAt.toLocalTime()),
                            ),
                            role = TextRole.Secondary,
                            modifier = Modifier.testTag("forecast_updatedValue"),
                        )
                    }
                }
                item(key = "tabs") {
                    AppTabs(
                        tabs = listOf(
                            TabItem(stringResource(R.string.forecast_tab_today)),
                            TabItem(stringResource(R.string.forecast_tab_week)),
                        ),
                        selectedIndex = state.tab.ordinal,
                        onSelect = { onEvent(ForecastEvent.TabSelected(ForecastTab.entries[it])) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forecast_tabs"),
                    )
                }
                // One item for both tabs' content, so the panel can travel with the selection:
                // no AnimatedContent can wrap two `items {}` blocks, and twenty-four rows
                // in a Column cost nothing a phone notices.
                item(key = "content") {
                    AppSwitcher(target = state.tab.ordinal) { tab ->
                        Column {
                            when (ForecastTab.entries[tab]) {
                                ForecastTab.Today -> {
                                    // The day as a line, with the rain under it: twenty-four rows
                                    // say every hour and none of them says the shape of the day.
                                    AppSparkline(
                                        values = state.todayTemperatures,
                                        bars = state.todayRain,
                                        contentDescription = stringResource(R.string.forecast_today_line),
                                        highlightIndex = state.currentHourIndex,
                                        modifier = Modifier
                                            .padding(AppTheme.spacing.inset.md)
                                            .testTag("forecast_todayValue"),
                                    )
                                    state.today.forEach { entry ->
                                        HourlyRow(
                                            entry = entry,
                                            unit = state.unit,
                                            modifier = Modifier.testTag("forecast_hourItem"),
                                        )
                                        AppDivider()
                                    }
                                }

                                ForecastTab.Week -> {
                                    val highs = state.weekHighs
                                    val lows = state.weekLows
                                    // Two lines, not one: a week of highs alone says nothing about
                                    // whether the nights are cold.
                                    AppSparkline(
                                        values = highs,
                                        values2 = lows,
                                        contentDescription = stringResource(
                                            R.string.forecast_week_line,
                                            lows.minOrNull()?.toInt() ?: 0,
                                            highs.maxOrNull()?.toInt() ?: 0,
                                        ),
                                        highlightIndex = 0,
                                        modifier = Modifier
                                            .padding(AppTheme.spacing.inset.md)
                                            // One id, not two: a second `testTag` on the same node
                                            // does not add an id, it loses both — checked, not
                                            // assumed. `weather.yaml` follows the rename.
                                            .testTag("forecast_weekValue"),
                                    )
                                    state.forecast.daily.forEach { entry ->
                                        DailyRow(
                                            entry = entry,
                                            unit = state.unit,
                                            modifier = Modifier.testTag("forecast_dayItem"),
                                        )
                                        AppDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview(
    @PreviewParameter(ForecastStatePreviews::class) state: ForecastState,
) = ThemedScreenPreview {
    ForecastScreen(
        state = state,
    ) {}
}
