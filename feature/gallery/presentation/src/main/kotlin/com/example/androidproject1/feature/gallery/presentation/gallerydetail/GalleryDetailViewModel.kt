package com.example.androidproject1.feature.gallery.presentation.gallerydetail

import com.example.androidproject1.feature.gallery.api.GalleryDetailDestination
import com.example.androidproject1.feature.gallery.presentation.galleryEntry
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger

class GalleryDetailViewModel(
    logger: Logger,
    // The route key, handed in by the destination. Available in `init`, and it comes back with
    // the back stack entry after process death.
    private val args: GalleryDetailDestination,
) : BaseViewModel<GalleryDetailState, GalleryDetailEvent, GalleryDetailNavigation>(
    initialState = galleryEntry(args.componentId)?.let {
        GalleryDetailState(
            componentId = it.id,
            name = it.name,
            group = it.group,
            summary = it.summary,
        )
    },
    logger = logger.withTag("GalleryDetailViewModel"),
) {

    override fun onUiEvent(event: GalleryDetailEvent) {
        when (event) {
            GalleryDetailEvent.NavigateUpClicked -> navigate(GalleryDetailNavigation.Up)
            is GalleryDetailEvent.ThemePicked -> updateData { copy(theme = event.theme) }
            // A foundation page has no bench entry of its own — the bench turns a component's
            // knobs, and a colour role has none — so the id it hands over is the one it was
            // opened with and the playground falls back to its first entry.
            GalleryDetailEvent.PlaygroundClicked ->
                navigate(GalleryDetailNavigation.OpenPlayground(args.componentId))
        }
    }
}
