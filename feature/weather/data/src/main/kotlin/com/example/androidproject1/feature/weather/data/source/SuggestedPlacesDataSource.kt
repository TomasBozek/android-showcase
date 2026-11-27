package com.example.androidproject1.feature.weather.data.source

import com.example.androidproject1.feature.weather.domain.Place

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:weather:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface SuggestedPlacesDataSource {

    /** The bundled list, whole and in its own order; the repository is what subtracts the saved. */
    fun suggestedPlaces(): List<Place>
}
