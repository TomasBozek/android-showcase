package com.example.androidproject1.feature.movies.domain

/** The movie list's cache, as far as anything above the data layer needs to know it. */
object MoviesCache {

    /**
     * How old the cached list may be before it is fetched again: a quarter of an hour. A launch
     * refreshes a list older than this, and so does a return to the app after being away
     * this long.
     */
    const val MAX_AGE_MILLIS = 15 * 60 * 1000L
}
