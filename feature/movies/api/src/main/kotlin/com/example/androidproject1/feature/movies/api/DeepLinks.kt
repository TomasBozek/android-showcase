package com.example.androidproject1.feature.movies.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import com.example.androidproject1.service.deeplink.bare
import com.example.androidproject1.service.deeplink.withId

/** `movies` and `movie/<id>`, a TVmaze show id under the list. */
val moviesDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "movies" to bare(MoviesDestination),
    "movie" to
        withId(String::toIntOrNull) { DeepLinkTarget(listOf(MoviesDestination, MovieDetailDestination(movieId = it))) },
)
