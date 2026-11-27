package com.example.androidproject1.feature.movies.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * A route that carries arguments. `create_screen.py --with-args` clones this set rather than the
 * plain one; the difference is entirely in this file, the ViewModel and the test.
 */
@Serializable
data class MovieDetailDestination(val movieId: Int) : NavKey
