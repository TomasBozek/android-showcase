package com.example.androidproject1.feature.settings.domain

import com.example.androidproject1.service.core.domain.result.Outcome

/** Counts what was asked of it; the rule itself is `DefaultReviewRepository`'s, tested there. */
class FakeReviewRepository : ReviewRepository {

    var requests = 0
        private set
    var milestones = 0
        private set

    override suspend fun requestReview(): Outcome<Unit> {
        requests++
        return Outcome.Success(Unit)
    }

    override suspend fun milestoneReached(): Outcome<Unit> {
        milestones++
        return Outcome.Success(Unit)
    }
}
