package com.example.androidproject1.core.ui.layout

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How a screen opens the main menu, provided once by the host around the tabbed flow — `null`
 * everywhere else, which is what a preview, a test and the auth flow see (the shape of
 * `LocalSharedTransitionScope`).
 *
 * `AppTopBar` is the only reader: a bar with no up arrow draws the menu icon when this is present,
 * so a tab root opens the drawer without being told about it.
 */
val LocalMenuOpener = staticCompositionLocalOf<(() -> Unit)?> { null }
