package com.example.androidproject1.feature.weather.presentation.weather

import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.layout.listPane
import com.example.androidproject1.feature.weather.api.ForecastDestination
import com.example.androidproject1.feature.weather.api.WeatherDestination
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.weatherDestination(navigator: Navigator) {
    // The list half of a list–detail pair: a wide window shows the detail beside it, and
    // the placeholder until something is picked; a phone is unchanged.
    entry<WeatherDestination>(
        metadata = listPane(
            detailPlaceholder = {
                AppEmptyState(
                    title = stringResource(R.string.forecast_placeholder_title),
                    message = stringResource(R.string.forecast_placeholder_message),
                )
            },
        ),
    ) {
        val viewModel: WeatherViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    is WeatherNavigation.Forecast -> navigator.push(ForecastDestination(placeId = navigation.placeId))
                    WeatherNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            WeatherScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
