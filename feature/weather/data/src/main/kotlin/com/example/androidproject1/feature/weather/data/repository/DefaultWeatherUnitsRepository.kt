package com.example.androidproject1.feature.weather.data.repository

import com.example.androidproject1.feature.weather.data.source.LocalWeatherUnitsDataSource
import com.example.androidproject1.feature.weather.domain.WeatherUnitsRepository
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.Flow

class DefaultWeatherUnitsRepository(
    logger: Logger,
    private val localWeatherUnitsDataSource: LocalWeatherUnitsDataSource,
) : WeatherUnitsRepository, BaseRepository(logger = logger.withTag("DefaultWeatherUnitsRepository")) {

    // `retries`, because the collector is a screen that outlives a failure: a flow that has
    // thrown is terminal, and one transient read error would leave the unit stuck for as long
    // as the screen lives.
    override fun observeUnit(): Flow<Outcome<TemperatureUnit>> =
        observe(source = localWeatherUnitsDataSource.observeUnit(), retries = RETRIES)

    override suspend fun setUnit(unit: TemperatureUnit): Outcome<Unit> = execute {
        localWeatherUnitsDataSource.setUnit(unit)
    }

    private companion object {

        const val RETRIES = 3L
    }
}
