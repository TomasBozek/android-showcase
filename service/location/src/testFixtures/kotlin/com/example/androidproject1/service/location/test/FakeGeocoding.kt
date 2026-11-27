package com.example.androidproject1.service.location.test

import com.example.androidproject1.service.location.Fix
import com.example.androidproject1.service.location.Geocoding

/** A [Geocoding] that answers from [names] (by fix), and is [isAvailable] or not as scripted. */
class FakeGeocoding(
    override var isAvailable: Boolean = true,
    var names: Map<Fix, String> = emptyMap(),
) : Geocoding {

    val asked = mutableListOf<Fix>()

    override suspend fun name(fix: Fix): String? {
        asked += fix
        return if (isAvailable) names[fix] else null
    }
}
