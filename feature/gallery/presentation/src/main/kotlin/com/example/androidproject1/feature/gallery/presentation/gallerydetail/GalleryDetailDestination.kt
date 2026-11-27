package com.example.androidproject1.feature.gallery.presentation.gallerydetail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.devmenu.api.DevMenuPlaygroundDestination
import com.example.androidproject1.feature.gallery.api.GalleryDetailDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.galleryDetailDestination(
    navigator: Navigator,
) {
    entry<GalleryDetailDestination> { key ->
        // The route key goes straight into the ViewModel through Koin, so it is available in
        // `init` and comes back with the entry after process death — neither of which is true of
        // a LaunchedEffect that calls a load() function.
        val viewModel: GalleryDetailViewModel = koinViewModel { parametersOf(key) }

        Screen(
            viewModel = viewModel,
            onNavigation = { navigation ->
                when (navigation) {
                    GalleryDetailNavigation.Up -> navigator.back()
                    // The bench is the dev menu's, reached by its api route.
                    is GalleryDetailNavigation.OpenPlayground ->
                        navigator.push(DevMenuPlaygroundDestination(navigation.componentId))
                }
            },
        ) { state, onEvent ->
            GalleryDetailScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
