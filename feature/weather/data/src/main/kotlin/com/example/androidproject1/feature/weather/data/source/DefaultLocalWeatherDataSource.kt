package com.example.androidproject1.feature.weather.data.source

import com.example.androidproject1.feature.weather.data.database.ForecastEntity
import com.example.androidproject1.feature.weather.data.database.WeatherDao
import com.example.androidproject1.feature.weather.data.database.toDomain
import com.example.androidproject1.feature.weather.data.database.toEntity
import com.example.androidproject1.feature.weather.domain.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The places and their forecasts, read from and written to the database. A forecast is stored
 * as the JSON the host sent and parsed on the way out, so the cache and the wire share one
 * shape and one mapper.
 */
class DefaultLocalWeatherDataSource(
    private val weatherDao: WeatherDao,
    private val now: () -> Long = System::currentTimeMillis,
) : LocalWeatherDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observePlaces(): Flow<List<Place>> = weatherDao.observePlaces().map { rows ->
        rows.map { it.toDomain() }
    }

    override suspend fun getPlace(placeId: Int): Place? = weatherDao.place(placeId)?.toDomain()

    override suspend fun savePlace(place: Place) = weatherDao.savePlace(place.toEntity(savedAt = now()))

    override suspend fun movePlace(placeId: Int, toIndex: Int) = weatherDao.movePlace(placeId, toIndex)

    override suspend fun removePlace(placeId: Int) {
        weatherDao.deletePlace(placeId)
        weatherDao.deleteForecast(placeId)
    }

    override fun observeForecast(placeId: Int): Flow<FetchedForecast?> =
        weatherDao.observeForecast(placeId).map { row ->
            row?.let {
                // The row's own `fetchedAt`, which is the one fact the JSON does not carry: a
                // forecast kept for offline has to be able to say how old it is.
                val fetchedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.fetchedAt), ZoneId.systemDefault())
                FetchedForecast(
                    json = it.json,
                    forecast = json.decodeFromString<ForecastDto>(it.json).toDomain(fetchedAt),
                )
            }
        }

    override suspend fun storeForecast(placeId: Int, forecast: FetchedForecast) =
        weatherDao.insertForecast(ForecastEntity(placeId = placeId, json = forecast.json, fetchedAt = now()))
}
