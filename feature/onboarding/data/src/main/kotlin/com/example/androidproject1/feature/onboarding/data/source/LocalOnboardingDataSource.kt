package com.example.androidproject1.feature.onboarding.data.source

import kotlinx.coroutines.flow.Flow

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:onboarding:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface LocalOnboardingDataSource {

    fun observeSeen(): Flow<Boolean>

    suspend fun setSeen(seen: Boolean)

    /** The tour's own flag, in the same store and read the same way. */
    fun observeTourSeen(): Flow<Boolean>

    suspend fun setTourSeen(seen: Boolean)

    /** `0` until something has been stored. */
    suspend fun lastSeenVersionCode(): Int

    suspend fun setLastSeenVersionCode(versionCode: Int)
}
