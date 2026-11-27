package com.example.androidproject1.feature.settings.data.source

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:settings:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface LocalReviewDataSource {

    /** Counts one more milestone and answers the new count. */
    suspend fun addMilestone(): Int

    /** Whether the automatic ask has happened on this install. */
    suspend fun hasAsked(): Boolean

    suspend fun markAsked()
}
