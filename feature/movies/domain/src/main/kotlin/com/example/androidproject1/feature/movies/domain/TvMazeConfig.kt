package com.example.androidproject1.feature.movies.domain

/**
 * Where TVmaze is: public, free and keyless, so this is a URL and nothing else. The host is
 * the same on every flavor, so it lives here and the feature's own `di` binds [DEFAULT] —
 * a copy that deletes the feature deletes the host with it.
 */
data class TvMazeConfig(
    val apiBaseUrl: String,
) {

    companion object {

        val DEFAULT = TvMazeConfig(apiBaseUrl = "https://api.tvmaze.com/")
    }
}
