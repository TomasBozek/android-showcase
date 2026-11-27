package com.example.androidproject1.feature.movies.data.source

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.androidproject1.feature.movies.data.database.MovieEntity
import com.example.androidproject1.feature.movies.domain.MoviesCache
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pages the list from TVmaze into the database, which is what the screen reads.
 *
 * Room is the source of truth and Paging reads it; this is the half that fills it. **Refresh**
 * fetches page 1 and makes it the whole cache — the other pages are gone until scrolled to again,
 * as they always were. **Append** fetches the page after the last one cached, until the
 * page that says it is the last — TVmaze's `404` past its end. **Prepend** never happens: the list only grows at its end.
 *
 * A failure is `MediatorResult.Error`, and it leaves the cache exactly as it was — Paging keeps
 * showing what it has and the load state says a refresh or an append failed, which is what the
 * screen's retry is for. That is the offline read.
 *
 * On launch the cache is refreshed only when it is older than [maxAgeMillis]: a warm start reads
 * the table and asks nobody, and a cold one asks.
 */
@OptIn(ExperimentalPagingApi::class)
class MoviesRemoteMediator(
    private val local: LocalMoviesDataSource,
    private val remote: RemoteMoviesDataSource,
    private val now: () -> Long = System::currentTimeMillis,
    private val maxAgeMillis: Long = MoviesCache.MAX_AGE_MILLIS,
) : RemoteMediator<Int, MovieEntity>() {

    override suspend fun initialize(): InitializeAction {
        val cached = local.cachedPages() ?: return InitializeAction.LAUNCH_INITIAL_REFRESH
        return if (now() - cached.firstFetchedAt < maxAgeMillis) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, MovieEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> FIRST_PAGE
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val cached = local.cachedPages() ?: return refreshOrEnd()
                if (cached.isComplete) return MediatorResult.Success(endOfPaginationReached = true)
                cached.lastPage + 1
            }
        }
        return try {
            val fetched = remote.getPopular(page)
            if (loadType == LoadType.REFRESH) local.replaceAll(fetched) else local.replacePage(fetched)
            MediatorResult.Success(endOfPaginationReached = fetched.isLast)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            MediatorResult.Error(failure)
        }
    }

    /** An append with nothing cached is a refresh that has not happened yet: say the list is not over. */
    private fun refreshOrEnd(): MediatorResult = MediatorResult.Success(endOfPaginationReached = false)

    companion object {

        private const val FIRST_PAGE = 1
    }
}
