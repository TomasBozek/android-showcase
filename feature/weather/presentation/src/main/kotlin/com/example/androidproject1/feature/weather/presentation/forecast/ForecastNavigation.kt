package com.example.androidproject1.feature.weather.presentation.forecast

/** One-off navigation intents, turned into back-stack calls in ForecastDestination. */
sealed interface ForecastNavigation {

    data object NavigateUp : ForecastNavigation

    /** The place is nobody's — not saved, not bundled, unknown to the geocoder: the places instead. */
    data object Weather : ForecastNavigation
}
