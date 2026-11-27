package com.example.androidproject1.feature.apphold.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The one screen the app is held behind — drawn by the shell instead of the navigation
 * display, never pushed onto it.
 *
 * @param storeUrl this app's store page, which only the shell knows: the update button opens it.
 */
@Serializable
data class AppHoldDestination(val reason: AppHoldReason, val storeUrl: String) : NavKey

/** Why the app is held: this build is too old to talk to the server, or the server is down on purpose. */
@Serializable
enum class AppHoldReason { UpdateRequired, Maintenance }
