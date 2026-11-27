package com.example.androidproject1.service.location

import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

/** Where the device is, to within [accuracyM] metres. */
public data class Fix(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
)

/**
 * The device's position, from the platform's own location manager — no Play services.
 *
 * Approximate location is what this asks for: it names a city, which is what the app needs, and a
 * user who grants "approximate" only has granted enough.
 */
public interface LocationProvider {

    /**
     * One fix, taken now. A failure is [LocationPermissionError] with no permission, or
     * [LocationUnavailableError] with no provider on or no fix inside the timeout.
     */
    public suspend fun current(): Outcome<Fix>

    /** Fixes as they arrive, until the collector goes. Empty, and finished, with no permission or provider. */
    public fun updates(): Flow<Fix>

    /** The newest fix the system already has, of any age, or `null`: instant, and possibly stale. */
    public suspend fun lastKnown(): Fix?
}

/** Neither `ACCESS_COARSE_LOCATION` nor `ACCESS_FINE_LOCATION` is held. */
public class LocationPermissionError : DomainError(message = "Location permission is not granted")

/** Location is off, or the device could not say where it is in time. */
public class LocationUnavailableError(message: String) : DomainError(message = message)
