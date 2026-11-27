package com.example.androidproject1.feature.movies.data.source

import com.example.androidproject1.feature.movies.domain.Movie
import com.example.androidproject1.feature.movies.domain.MovieDetail
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.minutes

/**
 * TVmaze's wire shape, deliberately not the domain's: a DTO changes when the API does, and mapping
 * it here is what stops that reaching a ViewModel. The list and the detail answer the same
 * object, so one DTO serves both. Field names are TVmaze's, so the fixtures under
 * `app/src/dev/res/raw` and a real answer parse the same way.
 */
@Serializable
data class ShowDto(
    val id: Int,
    val name: String,
    val genres: List<String> = emptyList(),
    val runtime: Int? = null,
    /** What a show without one fixed length runs on average — the detail's fallback for [runtime]. */
    val averageRuntime: Int? = null,
    /** ISO-8601, or `null` for a show that has not premiered. */
    val premiered: String? = null,
    val rating: ShowRatingDto = ShowRatingDto(),
    val image: ShowImageDto? = null,
    /** HTML — `<p>`, `<b>`, `<i>` and entities — or `null`. */
    val summary: String? = null,
)

@Serializable
data class ShowRatingDto(
    val average: Double? = null,
)

@Serializable
data class ShowImageDto(
    val medium: String? = null,
    val original: String? = null,
)

internal fun ShowDto.toDomain(): Movie = Movie(
    id = id,
    title = name,
    overview = summary.toPlainText(),
    posterUrl = image?.medium,
    releaseDate = premiered.toLocalDateOrNull(),
    rating = rating.average ?: 0.0,
)

internal fun ShowDto.toDetail(): MovieDetail = MovieDetail(
    movie = toDomain(),
    // `0` or nothing is "unknown", not a zero-minute show.
    runtime = (runtime ?: averageRuntime)?.takeIf { it > 0 }?.minutes,
    // TVmaze has no tagline.
    tagline = null,
    genres = genres,
    backdropUrl = image?.original,
)

private val TAG = Regex("<[^>]+>")
private val ENTITIES = mapOf(
    "&amp;" to "&",
    "&quot;" to "\"",
    "&#39;" to "'",
    "&apos;" to "'",
    "&lt;" to "<",
    "&gt;" to ">",
    "&nbsp;" to " ",
)

/** TVmaze's summary as the plain text the domain carries: tags gone, the common entities decoded. */
internal fun String?.toPlainText(): String {
    if (isNullOrBlank()) return ""
    var text = replace(TAG, "")
    ENTITIES.forEach { (entity, character) -> text = text.replace(entity, character) }
    return text.trim()
}

private fun String?.toLocalDateOrNull(): LocalDate? {
    if (isNullOrBlank()) return null
    return try {
        LocalDate.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
}
