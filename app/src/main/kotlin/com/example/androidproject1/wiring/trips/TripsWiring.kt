package com.example.androidproject1.wiring.trips

import com.example.androidproject1.feature.devmenu.presentation.ReminderTrigger
import com.example.androidproject1.feature.trips.domain.TripReminders
import com.example.androidproject1.feature.trips.domain.TripsRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.first
import org.koin.core.module.Module
import org.koin.dsl.module
import java.time.LocalDate

/**
 * The debug menu's "Fire a reminder", over the trips feature the menu cannot name.
 * `delete_feature.py trips` deletes this package and its include with it.
 */
val tripsDevMenuWiring: Module = module {
    single<ReminderTrigger> { TripReminderTrigger(get(), get()) }
}

/** Which trip: the one that starts soonest and has not finished — what a reminder would be for. */
internal class TripReminderTrigger(
    private val trips: TripsRepository,
    private val reminders: TripReminders,
    private val today: () -> LocalDate = LocalDate::now,
) : ReminderTrigger {

    override suspend fun fire(): ReminderTrigger.Fired {
        val all = (trips.observeTrips().first() as? Outcome.Success)?.data.orEmpty()
        val trip = all.filter { !it.endDate.isBefore(today()) }.minByOrNull { it.startDate }
            ?: return ReminderTrigger.Fired.NothingToRemind
        return if (reminders.remindNow(trip.id)) {
            ReminderTrigger.Fired.Posted(trip.name)
        } else {
            ReminderTrigger.Fired.Blocked
        }
    }
}
