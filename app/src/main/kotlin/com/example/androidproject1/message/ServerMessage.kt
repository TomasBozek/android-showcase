package com.example.androidproject1.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * One line from the server into the app without a release: a campaign, an outage notice,
 * a "rate us". Shown once per [id], only between [from] and [until], and never again once
 * dismissed.
 *
 * `:app`'s own, not a feature's: it is shown over whichever screen is up or in Home's header, and
 * its [action] is a deep link — both of which only the app can resolve.
 */
@Serializable
data class ServerMessage(
    val id: String,
    val placement: Placement,
    val title: String,
    val body: String,
    /** An app link (`<applicationId>://…`) the action key follows; none means no key. */
    val action: String? = null,
    /** ISO-8601 instants, `2026-10-01T00:00:00Z`. */
    val from: String,
    val until: String,
) {

    /** Whether [now] falls inside the message's window. A window that does not parse is closed. */
    fun isLive(now: Instant): Boolean = runCatching {
        !now.isBefore(Instant.parse(from)) && now.isBefore(Instant.parse(until))
    }.getOrDefault(false)

    @Serializable
    enum class Placement {
        /** Home's header, above the doors, until dismissed. */
        @SerialName("banner")
        Banner,

        /** A dialog at start, over whatever is up. */
        @SerialName("dialog")
        Dialog,
    }
}
