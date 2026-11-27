package com.example.androidproject1.feature.profile.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** A route that carries arguments: the destination hands it whole to the screen's ViewModel. */
@Serializable
data class AvatarCropDestination(val uri: String) : NavKey
