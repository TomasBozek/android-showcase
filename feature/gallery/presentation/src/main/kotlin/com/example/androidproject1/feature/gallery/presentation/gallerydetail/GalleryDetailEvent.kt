package com.example.androidproject1.feature.gallery.presentation.gallerydetail

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface GalleryDetailEvent : UiEvent {

    data object NavigateUpClicked : GalleryDetailEvent

    /** The light/dark strip above the variants. */
    data class ThemePicked(val theme: DetailTheme) : GalleryDetailEvent

    /** Hand this component to the bench, with its knobs. */
    data object PlaygroundClicked : GalleryDetailEvent
}
