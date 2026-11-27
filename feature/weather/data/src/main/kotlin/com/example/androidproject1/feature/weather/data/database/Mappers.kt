package com.example.androidproject1.feature.weather.data.database

import com.example.androidproject1.feature.weather.domain.Place

internal fun PlaceEntity.toDomain(): Place = Place(
    id = id,
    name = name,
    country = country,
    region = region,
    latitude = latitude,
    longitude = longitude,
    timezone = timezone,
)

/**
 * [position] is advisory: [WeatherDao.savePlace] decides the order inside its own transaction —
 * the end of the list for a new place, the place it already had for one saved twice — so a
 * caller that is not restoring a known order passes [POSITION_UNSET] and lets it.
 */
internal fun Place.toEntity(savedAt: Long, position: Int = POSITION_UNSET): PlaceEntity = PlaceEntity(
    id = id,
    name = name,
    country = country,
    region = region,
    latitude = latitude,
    longitude = longitude,
    timezone = timezone,
    savedAt = savedAt,
    position = position,
)

/** "The DAO decides": never stored, because `savePlace` replaces it before the insert. */
internal const val POSITION_UNSET = -1
