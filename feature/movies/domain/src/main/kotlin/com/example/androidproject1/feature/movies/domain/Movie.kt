package com.example.androidproject1.feature.movies.domain

import java.time.LocalDate

/**
 * One row of the list, a show from TVmaze. The wire shape is `ShowDto`, in the data layer;
 * the overview is plain text here, because the domain never learns that TVmaze sends HTML.
 */
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val releaseDate: LocalDate?,
    /** TVmaze's `rating.average`, 0–10, or 0 when it has none. */
    val rating: Double,
)
