package com.example.androidproject1.feature.movies.domain

/**
 * One page of the list — the unit the cache stores and the list appends.
 *
 * [isLast] rides on every page because the caller reading page `n` is the one that decides whether
 * there is an `n + 1`. TVmaze gives no total: a page is last when the one after it is a `404` or
 * empty, so the last page the list shows is followed by an empty one that says so.
 */
data class MoviePage(
    val page: Int,
    val movies: List<Movie>,
    val isLast: Boolean,
)
