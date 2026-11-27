package com.example.androidproject1.feature.gallery.presentation.gallery

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.feature.gallery.api.GalleryDestination
import com.example.androidproject1.feature.gallery.api.GalleryDetailDestination
import com.example.androidproject1.service.core.ui.component.Screen
import com.example.androidproject1.service.navigation.Navigator
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.galleryDestination(navigator: Navigator) {
    entry<GalleryDestination> {
        val viewModel: GalleryViewModel = koinViewModel()

        Screen(
            viewModel = viewModel,
            // Turn each GalleryNavigation case into a navigator call here — `navigator.push(Key)`
            // to push, `navigator.back()` to pop. Another feature is reached the same way, by the
            // route key in its api module.
            onNavigation = { navigation ->
                when (navigation) {
                    is GalleryNavigation.ToComponent ->
                        navigator.push(GalleryDetailDestination(navigation.id))

                    GalleryNavigation.NavigateUp -> navigator.back()
                }
            },
        ) { state, onEvent ->
            GalleryScreen(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}
