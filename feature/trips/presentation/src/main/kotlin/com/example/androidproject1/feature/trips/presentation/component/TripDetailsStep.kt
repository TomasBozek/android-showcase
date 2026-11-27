package com.example.androidproject1.feature.trips.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.choice.AppSegmented
import com.example.androidproject1.core.ui.component.choice.AppStepper
import com.example.androidproject1.core.ui.component.field.AppDateField
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.component.field.AppTimeField
import com.example.androidproject1.core.ui.component.list.AppAccordion
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.trips.domain.TripType
import com.example.androidproject1.feature.trips.presentation.R
import com.example.androidproject1.feature.trips.presentation.tripwizard.TripWizardEvent
import com.example.androidproject1.feature.trips.presentation.tripwizard.TripWizardState
import com.example.androidproject1.service.core.ui.text.resolve

/**
 * The wizard's first step: what the trip is called, what kind it is, when it runs and who is
 * coming. Its own file because `TripWizardScreen` composes three of these and a screen file holds
 * only the screen and its previews.
 */
@Composable
fun TripDetailsStep(
    state: TripWizardState,
    onEvent: (TripWizardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
    ) {
        AppTextField(
            value = state.name.value,
            errorText = state.name.error?.resolve(),
            onValueChange = { onEvent(TripWizardEvent.NameChanged(it)) },
            label = stringResource(R.string.trip_wizard_name_label),
            imeAction = ImeAction.Next,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tripWizard_nameField"),
        )

        val types = TripType.entries
        AppSegmented(
            options = types.map { stringResource(it.labelRes()) },
            selectedIndex = types.indexOf(state.type),
            onSelect = { onEvent(TripWizardEvent.TypeSelected(types[it])) },
            modifier = Modifier.testTag("tripWizard_typeField"),
        )

        AppDateField(
            value = state.startDate,
            onValueChange = { onEvent(TripWizardEvent.StartDateChanged(it)) },
            label = stringResource(R.string.trip_wizard_start_date_label),
            modifier = Modifier.testTag("tripWizard_startDateField"),
        )

        AppDateField(
            value = state.endDate,
            onValueChange = { onEvent(TripWizardEvent.EndDateChanged(it)) },
            label = stringResource(R.string.trip_wizard_end_date_label),
            modifier = Modifier.testTag("tripWizard_endDateField"),
        )

        AppStepper(
            value = state.travelers,
            onValueChange = { onEvent(TripWizardEvent.TravelersChanged(it)) },
            min = 1,
            modifier = Modifier.testTag("tripWizard_travelersField"),
        )

        AppAccordion(
            title = stringResource(R.string.trip_wizard_advanced_label),
            expanded = state.advancedExpanded,
            onToggle = { onEvent(TripWizardEvent.AdvancedToggled) },
            modifier = Modifier.testTag("tripWizard_advancedField"),
        ) {
            // The step's last field, so Done does what Next does.
            AppTextField(
                value = state.notes,
                onValueChange = { onEvent(TripWizardEvent.NotesChanged(it)) },
                label = stringResource(R.string.trip_wizard_notes_label),
                imeAction = ImeAction.Done,
                onImeAction = { onEvent(TripWizardEvent.NextClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tripWizard_notesField"),
            )
            // Optional, like the notes: a departure is a time of day on the first date,
            // from the platform's picker — a typed time is a format argument nobody wins.
            AppTimeField(
                value = state.departureTime,
                onValueChange = { onEvent(TripWizardEvent.DepartureTimeChanged(it)) },
                label = stringResource(R.string.trip_wizard_departure_time_label),
                modifier = Modifier.testTag("tripWizard_departureTimeField"),
            )
        }
    }
}

private fun TripType.labelRes() = when (this) {
    TripType.Leisure -> R.string.trip_wizard_type_leisure
    TripType.Business -> R.string.trip_wizard_type_business
    TripType.Family -> R.string.trip_wizard_type_family
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    TripDetailsStep(state = TripWizardState.PREVIEW, onEvent = {})
}
