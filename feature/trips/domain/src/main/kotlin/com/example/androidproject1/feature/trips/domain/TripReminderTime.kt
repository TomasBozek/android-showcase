package com.example.androidproject1.feature.trips.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** When a trip's reminder fires: 18:00, the evening before its first day. */
object TripReminderTime {

    val EVENING: LocalTime = LocalTime.of(18, 0)

    fun firesAt(startDate: LocalDate): LocalDateTime = startDate.minusDays(1).atTime(EVENING)

    /** Whether the evening is still ahead of [now]: a reminder can still be set. */
    fun isAhead(now: LocalDateTime, startDate: LocalDate): Boolean = delayFrom(now, startDate) != null

    /** How long from [now] until then, or `null` when it has already passed — there is nothing left to remind of. */
    fun delayFrom(now: LocalDateTime, startDate: LocalDate): Duration? =
        Duration.between(now, firesAt(startDate)).takeIf { !it.isNegative && !it.isZero }
}
