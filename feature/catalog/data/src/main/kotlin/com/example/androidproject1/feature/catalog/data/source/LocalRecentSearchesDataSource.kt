package com.example.androidproject1.feature.catalog.data.source

import kotlinx.coroutines.flow.Flow

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:catalog:data` names it. What the rest of the app depends on is
 * `RecentSearchesRepository`.
 */
interface LocalRecentSearchesDataSource {

    /** Most recent first. Empty until something has been searched for. */
    fun observeRecents(): Flow<List<String>>

    /** A snapshot, for the read-reorder-write a repeated search needs. */
    suspend fun read(): List<String>

    suspend fun replace(recents: List<String>)

    /** Removes the key outright — what deleting the account does, rather than an empty list. */
    suspend fun clear()
}
