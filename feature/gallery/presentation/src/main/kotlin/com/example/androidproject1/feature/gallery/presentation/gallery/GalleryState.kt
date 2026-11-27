package com.example.androidproject1.feature.gallery.presentation.gallery

import androidx.compose.runtime.Immutable
import com.example.androidproject1.feature.gallery.presentation.GalleryGroup
import com.example.androidproject1.feature.gallery.presentation.galleryItems

/** One row of the gallery list. Plain data — the composable demo is looked up by [id]. */
@Immutable
data class GalleryItem(
    val id: String,
    val name: String,
    val group: GalleryGroup,
    val summary: String,
)

@Immutable
data class GalleryState(
    val items: List<GalleryItem>,
) {
    /**
     * The list, in the order the design system groups them — the enum's own order, not the
     * order the catalogue happens to list them in.
     */
    val groups: List<Pair<GalleryGroup, List<GalleryItem>>>
        get() = items.groupBy { it.group }.toList().sortedBy { it.first.ordinal }

    companion object {

        val PREVIEW = GalleryState(items = galleryItems())
    }
}
