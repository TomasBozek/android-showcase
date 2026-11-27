package com.example.androidproject1.feature.weather.data.source

import com.example.androidproject1.feature.weather.domain.CurrentConditions
import com.example.androidproject1.feature.weather.domain.DailyEntry
import com.example.androidproject1.feature.weather.domain.Forecast
import com.example.androidproject1.feature.weather.domain.HourlyEntry
import com.example.androidproject1.feature.weather.domain.Place
import com.example.androidproject1.feature.weather.domain.WeatherCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Open-Meteo's wire shapes, deliberately not the domain's. Field names are Open-Meteo's, so the
 * fixtures under `app/src/dev/res/raw` and a real answer parse the same way; the forecast's
 * parallel arrays become one entry per hour and per day in the mapper.
 */
@Serializable
data class GeocodingResponseDto(
    /** Absent, not empty, when nothing matched — which is what the real geocoder sends. */
    val results: List<PlaceDto> = emptyList(),
)

@Serializable
data class PlaceDto(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = "",
    val admin1: String? = null,
    val timezone: String = "UTC",
)

@Serializable
data class ForecastDto(
    val current: CurrentDto,
    val hourly: HourlyDto,
    val daily: DailyDto,
)

@Serializable
data class CurrentDto(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
)

@Serializable
data class HourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int>,
    @SerialName("weather_code") val weatherCode: List<Int>,
)

@Serializable
data class DailyDto(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerialName("precipitation_sum") val precipitation: List<Double>,
)

internal fun PlaceDto.toDomain(): Place = Place(
    id = id,
    name = name,
    country = country,
    region = admin1?.takeIf { it.isNotBlank() && it != name },
    latitude = latitude,
    longitude = longitude,
    timezone = timezone,
)

internal fun ForecastDto.toDomain(fetchedAt: LocalDateTime): Forecast = Forecast(
    fetchedAt = fetchedAt,
    current = CurrentConditions(
        time = LocalDateTime.parse(current.time),
        temperature = current.temperature,
        code = WeatherCode.from(current.weatherCode),
        windSpeed = current.windSpeed,
    ),
    hourly = hourly.time.indices.map { i ->
        HourlyEntry(
            time = LocalDateTime.parse(hourly.time[i]),
            temperature = hourly.temperature[i],
            precipitationProbability = hourly.precipitationProbability[i] / PERCENT,
            code = WeatherCode.from(hourly.weatherCode[i]),
        )
    },
    daily = daily.time.indices.map { i ->
        DailyEntry(
            date = LocalDate.parse(daily.time[i]),
            code = WeatherCode.from(daily.weatherCode[i]),
            temperatureMax = daily.temperatureMax[i],
            temperatureMin = daily.temperatureMin[i],
            precipitation = daily.precipitation[i],
        )
    },
)

private const val PERCENT = 100.0
