package com.example.androidproject1.feature.gallery.presentation.gallerydetail

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.gallery.presentation.GalleryGroup
import com.example.androidproject1.feature.gallery.presentation.R

/**
 * Which theme the variants are drawn in.
 *
 * [System] is what the rest of the app is in, so a page opened from the list looks like the app;
 * the other two are the check — a role that reads in one theme and not the other is the failure
 * this strip exists to find, and it is a nested `AppTheme` rather than the playground's stage, which
 * a feature may not import.
 */
enum class DetailTheme(@StringRes val label: Int) {
    System(R.string.gallery_detail_theme_system),
    Light(R.string.gallery_detail_theme_light),
    Dark(R.string.gallery_detail_theme_dark),
}

@Immutable
data class GalleryDetailState(
    val componentId: String,
    val name: String,
    val group: GalleryGroup,
    val summary: String,
    val theme: DetailTheme = DetailTheme.System,
) {

    companion object {

        val PREVIEW = GalleryDetailState(
            componentId = "button",
            name = "AppButton",
            group = GalleryGroup.Action,
            summary = "Six kinds, three sizes.",
        )
    }
}
