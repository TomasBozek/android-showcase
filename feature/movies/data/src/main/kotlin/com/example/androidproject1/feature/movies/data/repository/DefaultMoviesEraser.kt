package com.example.androidproject1.feature.movies.data.repository

import com.example.androidproject1.feature.movies.data.database.MoviesDatabase
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.withContext

/**
 * The movies feature's share of "delete my account": every table of [MoviesDatabase].
 *
 * `clearAllTables` is blocking and Room refuses it on the main thread, so it runs on IO here —
 * the same rule the data sources follow, since `BaseRepository` runs on the caller's context.
 */
class DefaultMoviesEraser(
    private val database: MoviesDatabase,
    private val dispatcherProvider: DispatcherProvider,
) : LocalDataEraser {

    override suspend fun erase() = withContext(dispatcherProvider.io) {
        database.clearAllTables()
    }
}
