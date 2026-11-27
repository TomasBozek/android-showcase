package com.example.androidproject1.feature.weather.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved place: the geocoder's row as the user kept it.
 *
 * [position] is the order the user arranged, from 0; [savedAt] is kept because it is the only
 * record of when a place was added, and it is what the 1→2 migration derived the first positions
 * from. The list reads [position], never [savedAt].
 */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val country: String,
    val region: String?,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val savedAt: Long,
    val position: Int,
)

/**
 * A forecast as the host sent it, one per place. The wire JSON rather than thirty-one columns:
 * nothing queries inside a forecast — it is read whole and replaced whole — and a schema nobody
 * reads is a migration nobody wants. `fetchedAt` says how old it is.
 */
@Entity(tableName = "forecasts")
data class ForecastEntity(
    @PrimaryKey val placeId: Int,
    val json: String,
    val fetchedAt: Long,
)
