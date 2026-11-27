package com.example.androidproject1.feature.settings.data.repository

import com.example.androidproject1.feature.settings.data.source.LocalReviewDataSource
import com.example.androidproject1.feature.settings.domain.ReviewLauncher
import com.example.androidproject1.feature.settings.domain.ReviewRepository
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger

class DefaultReviewRepository(
    logger: Logger,
    private val localReviewDataSource: LocalReviewDataSource,
    private val reviewLauncher: ReviewLauncher,
) : ReviewRepository, BaseRepository(logger = logger.withTag("DefaultReviewRepository")) {

    override suspend fun requestReview(): Outcome<Unit> = execute {
        reviewLauncher.launch()
    }

    override suspend fun milestoneReached(): Outcome<Unit> = execute {
        val count = localReviewDataSource.addMilestone()
        if (count >= ReviewRepository.MILESTONES_BEFORE_ASKING && !localReviewDataSource.hasAsked()) {
            // Marked before the sheet, so a sheet that throws or a process killed under it still
            // counts as the one ask: asking twice is worse than asking never.
            localReviewDataSource.markAsked()
            reviewLauncher.launch()
        }
    }
}
