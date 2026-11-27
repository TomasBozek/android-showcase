package com.example.androidproject1.feature.weather.data.source

import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import kotlinx.coroutines.flow.Flow

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:weather:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface LocalWeatherUnitsDataSource {

    /** [TemperatureUnit.Celsius] until a unit has been stored, and again if a stored one is unreadable. */
    fun observeUnit(): Flow<TemperatureUnit>

    suspend fun setUnit(unit: TemperatureUnit)
}
