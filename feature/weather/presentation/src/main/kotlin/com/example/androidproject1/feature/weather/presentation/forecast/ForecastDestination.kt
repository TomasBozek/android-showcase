package com.example.androidproject1.feature.weather.presentation.forecast

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.layout.detailPane
import com.example.androidproject1.feature.weather.api.ForecastDestination
import com.example.androidproject1.feature.weather.api.WeatherDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.forecastDestination(navigator: Navigator) {
    // The detail half of its list's pair.
    entry<ForecastDestination>(metadata = detailPane()) { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: ForecastViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    ForecastNavigation.NavigateUp -> navigator.back()
                    // Back to the places when a link brought the user through them, else onto
                    // them: push drops a route equal to the one on top.
                    ForecastNavigation.Weather -> {
                        navigator.back()
                        navigator.push(WeatherDestination)
                    }
                }
            },
        ) { state, onEvent ->
            ForecastScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
