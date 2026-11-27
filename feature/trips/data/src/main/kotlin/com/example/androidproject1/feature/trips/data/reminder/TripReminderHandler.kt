package com.example.androidproject1.feature.trips.data.reminder

import android.net.Uri
import com.example.androidproject1.feature.trips.data.R
import com.example.androidproject1.feature.trips.domain.Trip
import com.example.androidproject1.feature.trips.domain.TripsRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.device.DeviceInfo
import com.example.androidproject1.service.notification.AppNotification
import com.example.androidproject1.service.notification.NotificationAction
import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.Notifier
import com.example.androidproject1.service.notification.PostResult
import com.example.androidproject1.service.work.WorkHandler
import com.example.androidproject1.service.work.WorkResult
import kotlinx.coroutines.flow.first

/**
 * What runs at 18:00 the evening before a trip: post the reminder, if the trip is still there and
 * its reminder is still on. A trip deleted or a switch turned off since the job was scheduled is
 * not an error — the job simply has nothing to say.
 */
class TripReminderHandler(
    private val tripsRepository: TripsRepository,
    private val work: TripReminderWork,
    private val notifier: Notifier,
    private val device: DeviceInfo,
) : WorkHandler {

    override val key: String = KEY

    override suspend fun run(input: Map<String, String>): WorkResult {
        val tripId = input[TRIP_ID] ?: return WorkResult.Failure
        if (!work.isOn(tripId).first()) return WorkResult.Success
        return when (postFor(tripId)) {
            // Refused or blocked is the user's setting: asking again at the next attempt would not change it.
            PostResult.Failed -> WorkResult.Retry
            else -> WorkResult.Success
        }
    }

    /** Posts it now; `true` when it was shown. For the debug menu, which does not care what the switch says. */
    suspend fun post(tripId: String): Boolean = postFor(tripId) == PostResult.Posted

    private suspend fun postFor(tripId: String): PostResult {
        val trip = (tripsRepository.getTrip(tripId) as? Outcome.Success)?.data ?: return PostResult.Blocked
        return notifier.post(notificationFor(trip))
    }

    private fun notificationFor(trip: Trip): AppNotification {
        val link = "${device.applicationId}://trip/${Uri.encode(trip.id)}"
        return AppNotification(
            kind = NotificationKind.Reminders,
            // One notification per trip: a second raise replaces it.
            id = trip.id.hashCode(),
            title = UiText.Resource(R.string.trip_reminder_title, listOf(trip.name)),
            text = UiText.Resource(R.string.trip_reminder_text, listOf(trip.destinationName)),
            deepLink = link,
            actions = listOf(NotificationAction(UiText.Resource(R.string.trip_reminder_open), link)),
            group = GROUP,
        )
    }

    companion object {

        const val KEY = "trip-reminder"
        const val TRIP_ID = "tripId"
        private const val GROUP = "trip-reminders"
    }
}
