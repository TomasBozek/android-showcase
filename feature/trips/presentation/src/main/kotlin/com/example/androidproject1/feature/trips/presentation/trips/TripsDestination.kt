package com.example.androidproject1.feature.trips.presentation.trips

import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.layout.listPane
import com.example.androidproject1.feature.trips.api.TripDetailDestination
import com.example.androidproject1.feature.trips.api.TripWizardDestination
import com.example.androidproject1.feature.trips.api.TripsDestination
import com.example.androidproject1.feature.trips.api.TripsListDestination
import com.example.androidproject1.feature.trips.presentation.R
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.tripsDestination(navigator: Navigator) {
    // The list half of a list–detail pair: a wide window shows the detail beside it, and
    // the placeholder until something is picked; a phone is unchanged.
    entry<TripsDestination>(
        metadata = listPane(
            detailPlaceholder = {
                AppEmptyState(
                    title = stringResource(R.string.trip_detail_placeholder_title),
                    message = stringResource(R.string.trip_detail_placeholder_message),
                )
            },
        ),
    ) {
        val viewModel: TripsViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    TripsNavigation.ViewAll -> navigator.push(TripsListDestination)
                    TripsNavigation.NewTrip -> navigator.push(TripWizardDestination)
                    is TripsNavigation.OpenTrip -> navigator.push(TripDetailDestination(navigation.tripId))
                }
            },
        ) { state, onEvent ->
            TripsScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
