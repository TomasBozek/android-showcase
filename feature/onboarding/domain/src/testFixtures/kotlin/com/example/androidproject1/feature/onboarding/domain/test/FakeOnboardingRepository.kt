package com.example.androidproject1.feature.onboarding.domain.test

import com.example.androidproject1.feature.onboarding.domain.OnboardingRepository
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [OnboardingRepository] for tests.
 *
 * A fixture of `:feature:onboarding:domain` rather than of one test source set, because the flag is
 * read in two places: the tour writes it, and `MainViewModel` decides the flow from it.
 *
 * @property seen settable, so a test can start from finished onboarding without running it.
 * @property tourSeen the tour's own flag, settable for the same reason.
 * @property failWith set to make [markSeen] and the flow fail.
 */
class FakeOnboardingRepository(var failWith: DomainError? = null) : OnboardingRepository {

    val seen = MutableStateFlow(false)

    override fun observeSeen(): Flow<Outcome<Boolean>> =
        seen.map { stored -> failWith?.let { Outcome.Failure(it) } ?: Outcome.Success(stored) }

    override suspend fun markSeen(): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        seen.value = true
        return Outcome.Success(Unit)
    }

    val tourSeen = MutableStateFlow(false)

    override fun observeTourSeen(): Flow<Outcome<Boolean>> =
        tourSeen.map { stored -> failWith?.let { Outcome.Failure(it) } ?: Outcome.Success(stored) }

    override suspend fun markTourSeen(): Outcome<Unit> = setTourSeen(true)

    override suspend fun resetTour(): Outcome<Unit> = setTourSeen(false)

    var lastSeenVersionCode = 0

    override suspend fun lastSeenVersionCode(): Outcome<Int> =
        failWith?.let { Outcome.Failure(it) } ?: Outcome.Success(lastSeenVersionCode)

    override suspend fun setLastSeenVersionCode(versionCode: Int): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        lastSeenVersionCode = versionCode
        return Outcome.Success(Unit)
    }

    private fun setTourSeen(value: Boolean): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        tourSeen.value = value
        return Outcome.Success(Unit)
    }
}
