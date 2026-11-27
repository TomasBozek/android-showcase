package com.example.androidproject1.feature.weather.data.repository

import com.example.androidproject1.feature.weather.data.database.WeatherDatabase
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.withContext

/**
 * The weather feature's share of "delete my account": every table of [WeatherDatabase].
 *
 * `clearAllTables` is blocking and Room refuses it on the main thread, so it runs on IO here —
 * the same rule the data sources follow, since `BaseRepository` runs on the caller's context.
 */
class DefaultWeatherEraser(
    private val database: WeatherDatabase,
    private val dispatcherProvider: DispatcherProvider,
) : LocalDataEraser {

    override suspend fun erase() = withContext(dispatcherProvider.io) {
        database.clearAllTables()
    }
}
