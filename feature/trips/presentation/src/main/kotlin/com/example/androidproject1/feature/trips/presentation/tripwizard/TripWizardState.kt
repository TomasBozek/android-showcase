package com.example.androidproject1.feature.trips.presentation.tripwizard

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.trips.domain.TripType
import com.example.androidproject1.feature.trips.presentation.R
import com.example.androidproject1.service.core.ui.form.FieldState
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.required
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText
import java.time.LocalDate
import java.time.LocalTime

@Immutable
data class TripWizardState(
    val step: Int = STEP_DETAILS,
    val name: FieldState = FieldState.of(required()),
    val type: TripType = TripType.Leisure,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val travelers: Int = 1,
    val advancedExpanded: Boolean = false,
    val notes: String = "",
    /** When the trip leaves on its first day; optional, under Advanced beside the notes. */
    val departureTime: LocalTime? = null,
    val destinationId: String? = null,
    val destinationName: String? = null,
    val budgetRange: ClosedFloatingPointRange<Float> = DEFAULT_BUDGET_RANGE,
) {

    /** Enough has been typed that leaving without asking would lose something. */
    val isDirty: Boolean
        get() = name.value.isNotBlank() || destinationId != null || notes.isNotBlank()

    val canContinue: Boolean
        get() = when (step) {
            STEP_DETAILS -> name.isValid && datesError == null
            STEP_DESTINATION -> destinationId != null
            else -> true
        }

    /**
     * The first thing wrong on this step — what the snackbar repeats when Next is pressed anyway.
     * The name is a field and carries its own error; the dates and the destination come
     * from pickers, which have no error line, so their reason lives here alone.
     */
    val firstError: UiText?
        get() = when (step) {
            STEP_DETAILS -> Form.firstError(name) ?: datesError
            STEP_DESTINATION -> R.string.trip_wizard_destination_required.toUiText().takeIf { destinationId == null }
            else -> null
        }

    private val datesError: UiText?
        get() = when {
            startDate == null -> R.string.trip_wizard_start_date_required.toUiText()
            endDate == null -> R.string.trip_wizard_end_date_required.toUiText()
            endDate.isBefore(startDate) -> R.string.trip_wizard_end_before_start.toUiText()
            else -> null
        }

    companion object {

        const val STEP_DETAILS = 0
        const val STEP_DESTINATION = 1
        const val STEP_REVIEW = 2
        const val STEP_COUNT = 3

        val DEFAULT_BUDGET_RANGE = 0.2f..0.5f

        const val BUDGET_MIN_MINOR = 0L
        const val BUDGET_MAX_MINOR = 20_000_00L

        /** Where a slider fraction sits between the two ends of the budget scale, in minor units. */
        fun budgetMinorFor(fraction: Float): Long =
            (BUDGET_MIN_MINOR + fraction * (BUDGET_MAX_MINOR - BUDGET_MIN_MINOR)).toLong()

        val PREVIEW = TripWizardState(
            step = STEP_DETAILS,
            name = FieldState.of(required(), value = "Summer in Lisbon"),
            type = TripType.Leisure,
            startDate = LocalDate.of(2026, 7, 10),
            endDate = LocalDate.of(2026, 7, 17),
            travelers = 2,
        )
    }
}
