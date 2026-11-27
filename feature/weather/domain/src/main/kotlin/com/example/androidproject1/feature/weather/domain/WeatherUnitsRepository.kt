package com.example.androidproject1.feature.weather.domain

import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

/**
 * Implemented in the data layer. Declared here so the domain layer depends on nothing.
 *
 * The unit a temperature is read in, kept apart from [WeatherRepository] because it is a
 * preference and not a place: it survives every place being removed, and switching it writes
 * nothing to the forecast cache.
 */
interface WeatherUnitsRepository {

    /** The unit the user picked; [TemperatureUnit.Celsius] until they pick one. */
    fun observeUnit(): Flow<Outcome<TemperatureUnit>>

    suspend fun setUnit(unit: TemperatureUnit): Outcome<Unit>
}
