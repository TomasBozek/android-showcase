package com.example.androidproject1.feature.settings.domain

import com.example.androidproject1.service.core.domain.result.Outcome

/**
 * When the app asks for a store rating. Implemented in the data layer, which keeps the count and
 * the once-only flag; the review sheet itself is [ReviewLauncher]'s.
 *
 * Nothing asks on open. A person is asked when they tap Rate, or once per install after the
 * [MILESTONES_BEFORE_ASKING]th time they finish something worth finishing — a trip saved.
 */
interface ReviewRepository {

    /** The user tapped Rate: show the sheet now, whatever has been asked before. */
    suspend fun requestReview(): Outcome<Unit>

    /**
     * Something worth asking after has happened. Asks on the [MILESTONES_BEFORE_ASKING]th, and never
     * again on this install, whether or not the user rated.
     */
    suspend fun milestoneReached(): Outcome<Unit>

    companion object {

        const val MILESTONES_BEFORE_ASKING = 3
    }
}
