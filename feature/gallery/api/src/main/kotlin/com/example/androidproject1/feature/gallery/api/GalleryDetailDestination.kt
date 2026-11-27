package com.example.androidproject1.feature.gallery.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** One component's page in the gallery, by its catalogue id. */
@Serializable
data class GalleryDetailDestination(val componentId: String) : NavKey
