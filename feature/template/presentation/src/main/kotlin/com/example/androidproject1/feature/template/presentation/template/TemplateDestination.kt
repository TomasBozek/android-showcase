package com.example.androidproject1.feature.template.presentation.template

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.template.api.TemplateDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.templateDestination(navigator: Navigator) {
    // A list that opens a detail declares itself the list pane, with what the detail pane shows
    // before anything is picked — `entry<Key>(metadata = listPane(detailPlaceholder = { … }))`
    // (RECIPES). Any other screen declares nothing: a list pane draws a second pane.
    entry<TemplateDestination> {
        val viewModel: TemplateViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            // Turn each TemplateNavigation case into a navigator call here — `navigator.push(Key)`
            // to push, `navigator.back()` to pop. Another feature is reached the same way, by the
            // route key in its api module.
            onNavigation = { navigation ->
            },
        ) { state, onEvent ->
            TemplateScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
