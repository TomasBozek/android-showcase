package com.example.androidproject1.feature.onboarding.data.repository

import com.example.androidproject1.feature.onboarding.data.source.LocalOnboardingDataSource
import com.example.androidproject1.feature.onboarding.domain.OnboardingRepository
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.Flow

class DefaultOnboardingRepository(
    logger: Logger,
    private val localOnboardingDataSource: LocalOnboardingDataSource,
) : OnboardingRepository, BaseRepository(logger = logger.withTag("DefaultOnboardingRepository")) {

    // `retries`, because the collector is `MainViewModel` and lives as long as the process: an
    // `observe` failure is terminal, so one transient read error would strand the app on the
    // splash screen. Same reason `DefaultAuthRepository.observeSession()` passes it.
    override fun observeSeen(): Flow<Outcome<Boolean>> =
        observe(source = localOnboardingDataSource.observeSeen(), retries = RETRIES)

    override suspend fun markSeen(): Outcome<Unit> = execute {
        localOnboardingDataSource.setSeen(true)
    }

    // `retries` for the same reason: the collector is the tour's view model, which lives as long
    // as the activity, and a terminal failure would leave the tour unable to start ever again.
    override fun observeTourSeen(): Flow<Outcome<Boolean>> =
        observe(source = localOnboardingDataSource.observeTourSeen(), retries = RETRIES)

    override suspend fun markTourSeen(): Outcome<Unit> = execute {
        localOnboardingDataSource.setTourSeen(true)
    }

    override suspend fun resetTour(): Outcome<Unit> = execute {
        localOnboardingDataSource.setTourSeen(false)
    }

    override suspend fun lastSeenVersionCode(): Outcome<Int> = execute {
        localOnboardingDataSource.lastSeenVersionCode()
    }

    override suspend fun setLastSeenVersionCode(versionCode: Int): Outcome<Unit> = execute {
        localOnboardingDataSource.setLastSeenVersionCode(versionCode)
    }

    private companion object {

        const val RETRIES = 3L
    }
}
