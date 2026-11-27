package com.example.androidproject1.feature.gallery.presentation.gallerydetail

sealed interface GalleryDetailNavigation {

    data object Up : GalleryDetailNavigation

    /**
     * Open the dev menu's bench on this component.
     *
     * The id is the gallery's, which is also the bench's: both catalogues key an entry by the
     * component's name in camelCase, so the gallery hands over a string the playground already
     * knows. A component with no bench entry opens the bench at its first, which is what the
     * playground does with any id it does not have.
     */
    data class OpenPlayground(val componentId: String) : GalleryDetailNavigation
}
