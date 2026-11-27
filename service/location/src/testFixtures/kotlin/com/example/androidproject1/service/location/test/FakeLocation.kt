package com.example.androidproject1.service.location.test

import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.location.Fix
import com.example.androidproject1.service.location.LocationProvider
import com.example.androidproject1.service.location.LocationUnavailableError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A [LocationProvider] that walks a scripted [route]: [current] is the next fix (the last one
 * repeats), [updates] plays the whole route, [lastKnown] is where it has got to. An empty route is a
 * device with no fix; set [failure] to script an error instead.
 */
class FakeLocation(var route: List<Fix> = emptyList()) : LocationProvider {

    var failure: Outcome.Failure? = null

    private var position = -1

    override suspend fun current(): Outcome<Fix> {
        failure?.let { return it }
        if (route.isEmpty()) return Outcome.Failure(LocationUnavailableError("no fix scripted"))
        position = (position + 1).coerceAtMost(route.lastIndex)
        return Outcome.Success(route[position])
    }

    override fun updates(): Flow<Fix> = flow { route.forEach { emit(it) } }

    override suspend fun lastKnown(): Fix? = route.getOrNull(position)
}
