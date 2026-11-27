package com.example.androidproject1.feature.trips.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.choice.AppRangeSlider
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppDescriptionList
import com.example.androidproject1.core.ui.component.list.DescriptionRow
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.trips.presentation.R
import com.example.androidproject1.feature.trips.presentation.tripwizard.TripWizardEvent
import com.example.androidproject1.feature.trips.presentation.tripwizard.TripWizardState
import com.example.androidproject1.service.locale.LocalFormats

/** The wizard's last step: the budget range, and everything chosen so far to look over before saving. */
@Composable
fun TripReviewStep(
    state: TripWizardState,
    onEvent: (TripWizardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formats = LocalFormats.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppRangeSlider(
                value = state.budgetRange,
                onValueChange = { onEvent(TripWizardEvent.BudgetRangeChanged(it)) },
                label = stringResource(R.string.trip_wizard_budget_label),
                valueLabel = "${formats.moneyShort(TripWizardState.budgetMinorFor(state.budgetRange.start))} – " +
                    formats.moneyShort(TripWizardState.budgetMinorFor(state.budgetRange.endInclusive)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("tripWizard_budgetField"),
            )
        }
        // The note beside an info icon whose tooltip says the same — the icon is where a pointer
        // or a long press finds it again once the note has scrolled by.
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(
                text = stringResource(R.string.trip_wizard_budget_tooltip),
                role = TextRole.Secondary,
                modifier = Modifier.weight(1f),
            )
            AppIconButton(
                icon = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.trip_wizard_budget_tooltip),
                onClick = {},
                size = ControlSize.Small,
                modifier = Modifier.testTag("tripWizard_budgetInfoButton"),
            )
        }

        AppDescriptionList(
            rows = listOfNotNull(
                DescriptionRow(stringResource(R.string.trip_detail_destination_label), state.destinationName),
                DescriptionRow(
                    stringResource(R.string.trip_detail_dates_label),
                    if (state.startDate != null && state.endDate != null) {
                        "${formats.date(state.startDate)} – ${formats.date(state.endDate)}"
                    } else {
                        null
                    },
                ),
                // Only when one was chosen: a row that says "—" for a thing nobody set is noise.
                state.departureTime?.let {
                    DescriptionRow(
                        stringResource(R.string.trip_detail_departure_time),
                        formats.time(it),
                        numeric = true,
                    )
                },
                DescriptionRow(
                    stringResource(R.string.trip_detail_travelers_label),
                    state.travelers.toString(),
                    numeric = true,
                ),
            ),
            modifier = Modifier.testTag("tripWizard_reviewTile"),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    TripReviewStep(
        state = TripWizardState.PREVIEW.copy(destinationId = "lisbon", destinationName = "Lisbon, Portugal"),
        onEvent = {},
    )
}
