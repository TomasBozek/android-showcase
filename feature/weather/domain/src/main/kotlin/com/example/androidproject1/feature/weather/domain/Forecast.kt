package com.example.androidproject1.feature.weather.domain

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The three shapes one Open-Meteo answer holds: now, the next hours, the next days — and when it
 * was fetched.
 *
 * [fetchedAt] is the row's own timestamp rather than anything in the answer, so it says how old
 * what is on screen is. It was stored from the first day and never read out: a forecast
 * kept for offline is worth nothing if the screen cannot say it is from this morning. A
 * `LocalDateTime` like every other time here — the app has no `Instant` and `Formats` has no role
 * that takes one.
 */
data class Forecast(
    val current: CurrentConditions,
    val hourly: List<HourlyEntry>,
    val daily: List<DailyEntry>,
    val fetchedAt: LocalDateTime,
)

data class CurrentConditions(
    val time: LocalDateTime,
    val temperature: Double,
    val code: WeatherCode,
    /** km/h. */
    val windSpeed: Double,
)

data class HourlyEntry(
    val time: LocalDateTime,
    val temperature: Double,
    /** 0–1. */
    val precipitationProbability: Double,
    val code: WeatherCode,
)

data class DailyEntry(
    val date: LocalDate,
    val code: WeatherCode,
    val temperatureMax: Double,
    val temperatureMin: Double,
    /** mm. */
    val precipitation: Double,
)
