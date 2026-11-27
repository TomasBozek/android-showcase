package com.example.androidproject1.feature.trips.presentation.tripwizard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.settings.domain.ReviewRepository
import com.example.androidproject1.feature.trips.domain.DestinationsRepository
import com.example.androidproject1.feature.trips.domain.Trip
import com.example.androidproject1.feature.trips.domain.TripType
import com.example.androidproject1.feature.trips.domain.TripsRepository
import com.example.androidproject1.service.core.domain.FormDrafts
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.form.ALERT_ID_DISCARD
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.discardAlert
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import com.example.androidproject1.service.core.ui.R as CoreR

class TripWizardViewModel(
    logger: Logger,
    private val tripsRepository: TripsRepository,
    private val destinationsRepository: DestinationsRepository,
    private val reviewRepository: ReviewRepository,
    savedStateHandle: SavedStateHandle,
    private val formDrafts: FormDrafts,
) : BaseViewModel<TripWizardState, TripWizardEvent, TripWizardNavigation>(
    // The wizard has nothing to load — it draws from an empty form immediately.
    initialState = TripWizardState(),
    logger = logger.withTag("TripWizardViewModel"),
    savedStateHandle = savedStateHandle,
) {

    // The wizard as one snapshot, written on every change: the steps behind it, the step it is on
    // and the destination picked. Twice — in saved state, which a process death hands back as it
    // was, and in `formDrafts`, which a relaunch finds and says so.
    private var savedDraft: String? by saved(KEY_DRAFT, null)

    /** Saved or discarded: nothing typed is waiting any more, so nothing is written after it. */
    private var finished = false

    init {
        val resumed = savedDraft?.let(::decode)
        if (resumed != null) {
            updateData { resumed.applyTo(this) }
        } else {
            viewModelScope.launch {
                val stored = formDrafts.read(KEY_DRAFT)?.let(::decode) ?: return@launch
                updateData { stored.applyTo(this) }
                showSnackbar(
                    message = CoreR.string.core_draft_restored.toUiText(),
                    actionLabel = CoreR.string.core_action_discard.toUiText(),
                    id = SNACKBAR_ID_DRAFT,
                )
            }
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_DISCARD) {
            uiState.clearAlert()
            finished = true
            forget()
            navigate(TripWizardNavigation.NavigateUp)
            return
        }
        if (event is SystemEvent.SnackbarAction && event.id == SNACKBAR_ID_DRAFT) {
            updateData { TripWizardState() }
            forget()
            return
        }
        super.onSystemEvent(event)
    }

    override fun onUiEvent(event: TripWizardEvent) {
        handle(event)
        remember()
    }

    private fun handle(event: TripWizardEvent) {
        when (event) {
            is TripWizardEvent.NameChanged -> updateData { copy(name = name.changed(event.name)) }
            is TripWizardEvent.TypeSelected -> updateData { copy(type = event.type) }
            is TripWizardEvent.StartDateChanged -> updateData { copy(startDate = event.date) }
            is TripWizardEvent.EndDateChanged -> updateData { copy(endDate = event.date) }
            is TripWizardEvent.TravelersChanged -> updateData { copy(travelers = event.count) }
            TripWizardEvent.AdvancedToggled -> updateData { copy(advancedExpanded = !advancedExpanded) }
            is TripWizardEvent.NotesChanged -> updateData { copy(notes = event.notes) }
            is TripWizardEvent.DepartureTimeChanged -> updateData { copy(departureTime = event.time) }
            is TripWizardEvent.BudgetRangeChanged -> updateData { copy(budgetRange = event.range) }
            TripWizardEvent.PickDestinationClicked -> navigate(TripWizardNavigation.PickDestination)
            TripWizardEvent.NextClicked -> next()
            TripWizardEvent.NavigateUpClicked -> back()
            TripWizardEvent.BackRequested -> uiState.setAlert(discardAlert())
        }
    }

    /** The picker's result — resolved to a name here, since the picker hands back only an id. */
    fun onDestinationPicked(destinationId: String) = execute(
        errorDisplay = ErrorDisplay.Silent,
        action = { destinationsRepository.getDestination(destinationId) },
        onData = { destination ->
            if (destination != null) {
                updateData { copy(destinationId = destination.id, destinationName = destination.name) }
                remember()
            }
        },
    )

    /**
     * Next stays enabled on an invalid step on purpose: pressing it touches every field,
     * which is what reveals the errors, and the first one is repeated in a snackbar — the only
     * place a missing date or destination can be said, since a picker has no error line.
     */
    private fun next() {
        val state = uiState.value.data ?: return
        if (!state.canContinue) {
            val (name) = Form.touchAll(state.name)
            val revealed = state.copy(name = name)
            updateData { revealed }
            revealed.firstError?.let { showSnackbar(it) }
            return
        }
        if (state.step == TripWizardState.STEP_REVIEW) {
            save()
        } else {
            updateData { copy(step = step + 1) }
        }
    }

    private fun back() {
        val state = uiState.value.data ?: return
        if (state.step > TripWizardState.STEP_DETAILS) {
            updateData { copy(step = step - 1) }
        } else {
            navigate(TripWizardNavigation.NavigateUp)
        }
    }

    private fun save() {
        val state = uiState.value.data ?: return
        val startDate = state.startDate ?: return
        val endDate = state.endDate ?: return
        val destinationId = state.destinationId ?: return

        execute(
            loading = overlay(),
            action = {
                tripsRepository.saveTrip(
                    Trip(
                        id = UUID.randomUUID().toString(),
                        name = state.name.value.trim(),
                        destinationId = destinationId,
                        destinationName = state.destinationName.orEmpty(),
                        type = state.type,
                        startDate = startDate,
                        endDate = endDate,
                        travelers = state.travelers,
                        budgetMinMinor = TripWizardState.budgetMinorFor(state.budgetRange.start),
                        budgetMaxMinor = TripWizardState.budgetMinorFor(state.budgetRange.endInclusive),
                        notes = state.notes,
                        departureTime = state.departureTime,
                    ),
                )
            },
            onData = {
                finished = true
                forget()
                navigate(TripWizardNavigation.Saved)
                milestone()
            },
        )
    }

    // A trip saved is the moment the app may ask for a rating — the repository decides whether
    // this is the one (the third, once per install). Silent: a store that failed to say
    // "rate us" is not something the user should hear about.
    private fun milestone() = execute(
        errorDisplay = ErrorDisplay.Silent,
        action = { reviewRepository.milestoneReached() },
        onData = {},
    )

    // A wizard with nothing in it is no draft: opening it and leaving must not leave one behind.
    private fun remember() {
        val state = uiState.value.data ?: return
        if (finished) return
        if (!state.isDirty) {
            forget()
            return
        }
        val encoded = Json.encodeToString(Draft.serializer(), Draft.of(state))
        savedDraft = encoded
        persist { formDrafts.write(KEY_DRAFT, encoded) }
    }

    private fun forget() {
        savedDraft = null
        persist { formDrafts.clear(KEY_DRAFT) }
    }

    // Not cancellable: the wizard closes as it saves or discards, and a write cut off there would
    // bring the draft back on the next launch.
    private fun persist(write: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { write() } }
    }

    private fun decode(encoded: String): Draft? =
        runCatching { Json.decodeFromString(Draft.serializer(), encoded) }.getOrNull()

    /** What the user has put into the wizard, and the step they were on. */
    @Serializable
    private data class Draft(
        val step: Int,
        val name: String,
        val type: TripType,
        val startEpochDay: Long?,
        val endEpochDay: Long?,
        val travelers: Int,
        val advancedExpanded: Boolean,
        val notes: String,
        val departureSecondOfDay: Int?,
        val destinationId: String?,
        val destinationName: String?,
        val budgetStart: Float,
        val budgetEnd: Float,
    ) {

        fun applyTo(state: TripWizardState) = state.copy(
            step = step,
            name = state.name.changed(name),
            type = type,
            startDate = startEpochDay?.let(LocalDate::ofEpochDay),
            endDate = endEpochDay?.let(LocalDate::ofEpochDay),
            travelers = travelers,
            advancedExpanded = advancedExpanded,
            notes = notes,
            departureTime = departureSecondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) },
            destinationId = destinationId,
            destinationName = destinationName,
            budgetRange = budgetStart..budgetEnd,
        )

        companion object {

            fun of(state: TripWizardState) = Draft(
                step = state.step,
                name = state.name.value,
                type = state.type,
                startEpochDay = state.startDate?.toEpochDay(),
                endEpochDay = state.endDate?.toEpochDay(),
                travelers = state.travelers,
                advancedExpanded = state.advancedExpanded,
                notes = state.notes,
                departureSecondOfDay = state.departureTime?.toSecondOfDay(),
                destinationId = state.destinationId,
                destinationName = state.destinationName,
                budgetStart = state.budgetRange.start,
                budgetEnd = state.budgetRange.endInclusive,
            )
        }
    }

    private companion object {

        const val KEY_DRAFT = "trip_wizard_draft"
        const val SNACKBAR_ID_DRAFT = "trip_wizard_draft_restored"
    }
}
