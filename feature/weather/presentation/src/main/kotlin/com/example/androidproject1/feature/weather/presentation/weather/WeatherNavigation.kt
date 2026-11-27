package com.example.androidproject1.feature.weather.presentation.weather

/** One-off navigation intents, turned into back-stack calls in WeatherDestination. */
sealed interface WeatherNavigation {

    data class Forecast(val placeId: Int) : WeatherNavigation

    data object NavigateUp : WeatherNavigation
}
