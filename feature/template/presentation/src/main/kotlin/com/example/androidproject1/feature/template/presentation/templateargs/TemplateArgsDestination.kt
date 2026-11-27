package com.example.androidproject1.feature.template.presentation.templateargs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.layout.detailPane
import com.example.androidproject1.feature.template.api.TemplateArgsDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The screen of a route that carries arguments, [TemplateArgsDestination]. `create_screen.py
 * --with-args` clones this set rather than the plain one; the difference is entirely in the key,
 * this file, the ViewModel and the test.
 */
fun EntryProviderScope<NavKey>.templateArgsDestination(navigator: Navigator) {
    // A screen of one thing, reached from a list: the detail pane, which a wide window shows beside
    // that list. Reached any other way it fills the window, so the metadata is safe to keep.
    entry<TemplateArgsDestination>(metadata = detailPane()) { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: TemplateArgsViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
            },
        ) { state, onEvent ->
            TemplateArgsScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
