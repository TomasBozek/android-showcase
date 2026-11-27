package com.example.androidproject1.feature.trips.presentation.tripdetail

import com.example.androidproject1.feature.trips.api.TripDetailDestination
import com.example.androidproject1.feature.trips.domain.Trip
import com.example.androidproject1.feature.trips.domain.TripReminderTime
import com.example.androidproject1.feature.trips.domain.TripReminders
import com.example.androidproject1.feature.trips.domain.TripsRepository
import com.example.androidproject1.feature.trips.presentation.R
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.event.ExternalTarget
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class TripDetailViewModel(
    logger: Logger,
    // The route key, handed in by the destination. Available in `init`, and it comes back with
    // the back stack entry after process death.
    private val args: TripDetailDestination,
    private val tripsRepository: TripsRepository,
    private val tripReminders: TripReminders,
    private val clock: Clock,
) : BaseViewModel<TripDetailState, TripDetailEvent, TripDetailNavigation>(
    // The route key is already enough to draw with; the trip itself fills in once it loads.
    initialState = TripDetailState(tripId = args.tripId, today = LocalDate.now(clock)),
    logger = logger.withTag("TripDetailViewModel"),
) {

    init {
        loadTrip()
        // Followed, not read once: the switch is this screen's, but the reminder is the data
        // layer's, and it can go off from elsewhere (the trip's evening passing, a deletion).
        observe(
            flow = { tripReminders.isOn(args.tripId).map<Boolean, Outcome<Boolean>> { Outcome.Success(it) } },
            errorDisplay = ErrorDisplay.Silent,
        ) { on -> updateData { copy(reminderOn = on) } }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_DELETE) {
            uiState.clearAlert()
            deleteTrip()
            return
        }
        super.onSystemEvent(event)
    }

    override fun onUiEvent(event: TripDetailEvent) {
        when (event) {
            is TripDetailEvent.TabSelected -> updateData { copy(selectedTab = event.index) }

            TripDetailEvent.DeleteClicked -> uiState.setAlert(
                id = ALERT_ID_DELETE,
                title = R.string.trip_detail_delete_title.toUiText(),
                message = R.string.trip_detail_delete_message.toUiText(),
                destructive = true,
                confirmLabel = R.string.trip_detail_delete_confirm.toUiText(),
                declineLabel = R.string.trip_detail_delete_cancel.toUiText(),
            )

            TripDetailEvent.MapClicked -> open { trip -> ExternalTarget.Map(trip.destinationName) }

            TripDetailEvent.CalendarClicked -> open { trip ->
                ExternalTarget.CalendarEvent(
                    title = trip.name,
                    start = trip.startDate,
                    endInclusive = trip.endDate,
                    location = trip.destinationName,
                )
            }

            is TripDetailEvent.ReminderToggled -> execute(
                errorDisplay = ErrorDisplay.Silent,
                action = { Outcome.Success(tripReminders.set(args.tripId, event.on)) },
                onData = {},
            )

            TripDetailEvent.NavigateUpClicked -> navigate(TripDetailNavigation.NavigateUp)
        }
    }

    /** Hands the shell something to open about the loaded trip; before it loads there is nothing. */
    private fun open(target: (Trip) -> ExternalTarget) {
        val trip = uiState.value.data?.trip ?: return
        sendCommand(UiCommand.OpenExternal(target(trip)))
    }

    private fun loadTrip() = execute(
        loading = overlay(),
        errorDisplay = ErrorDisplay.Inline,
        action = { tripsRepository.getTrip(args.tripId) },
        onData = { trip ->
            if (trip != null) {
                updateData {
                    copy(
                        trip = trip,
                        reminderAvailable = TripReminderTime.isAhead(LocalDateTime.now(clock), trip.startDate),
                    )
                }
            } else {
                // Gone — deleted from another screen, or a stale link. Nothing left to show here.
                navigate(TripDetailNavigation.NavigateUp)
            }
        },
    )

    private fun deleteTrip() = execute(
        loading = overlay(),
        action = { tripsRepository.deleteTrip(args.tripId) },
        onData = { navigate(TripDetailNavigation.NavigateUp) },
    )

    private companion object {

        const val ALERT_ID_DELETE = "delete_trip"
    }
}
