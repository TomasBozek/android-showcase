package com.example.androidproject1.feature.movies.data.source

import com.example.androidproject1.feature.movies.domain.MovieDetail
import com.example.androidproject1.feature.movies.domain.MoviePage

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:movies:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface RemoteMoviesDataSource {

    /**
     * The list's page [page], counted from 1 like the cache's (page 0 is the cache's row for a show
     * no page listed); TVmaze's own pages count from 0. A page past the last is an empty page with
     * `isLast` — TVmaze answers `404`.
     */
    suspend fun getPopular(page: Int): MoviePage

    /** `shows/{id}`. */
    suspend fun getMovie(id: Int): MovieDetail
}
