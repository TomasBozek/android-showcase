package com.example.androidproject1.feature.devmenu.presentation.devmenu

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.feature.devmenu.api.DevMenuDestination
import com.example.androidproject1.feature.devmenu.api.DevMenuPlaygroundDestination
import com.example.androidproject1.service.devtools.BuildInfo
import com.example.androidproject1.service.devtools.ConfigAnswer

/**
 * @property session the signed-in address, or `null` while signed out.
 * @property offlineSupported whether this flavor talks to fixtures and so has a switch to offer.
 * @property realApiSupported the same, for the switch that sends the third-party hosts' requests out.
 * @property latencyMs how long every fixture answer waits, and [failureRate] the share that are a
 * 503 — the engine's two knobs, read back after every write like the switches.
 * @property jumps the screens the menu opens directly, by their screen id.
 */
@Immutable
data class DevMenuState(
    val build: BuildInfo,
    val session: String?,
    val offlineSupported: Boolean,
    val offline: Boolean,
    val realApiSupported: Boolean,
    val realApi: Boolean,
    /** The fixture engine's two knobs: drawn with the offline switch, meaningless without it. */
    val latencyMs: Int = 0,
    val failureRate: Int = 0,
    /** The fixture's one-shot `401`: drawn with the offline switch. */
    val sessionExpiryArmed: Boolean = false,
    /** The startup config's answers switched on; drawn with the offline switch. */
    val configAnswers: Set<ConfigAnswer> = emptySet(),
    /** The fixture's orders on the fast clock; drawn with the offline switch. */
    val fastOrderClock: Boolean = false,
    /** Google Pay pretended to be ready; drawn with the offline switch. */
    val pretendGooglePay: Boolean = false,
    /** The `X-Request-Id` of the last request that failed, for a ticket to quote; `null` until one has. */
    val lastFailedRequestId: String? = null,
    /** How many crash and ANR records the phone keeps. */
    val crashReports: Int = 0,
    val jumps: List<ShellJump>,
    /** Whether the app has sign-in, and so a session row to draw. */
    val sessionShown: Boolean = false,
    /** Whether the app has a reminder to fire, and so a button for it. */
    val reminderShown: Boolean = false,
    /** Whether the app has a What's new sheet, and so a button to reset it. */
    val whatsNewShown: Boolean = false,
) {

    companion object {

        val PREVIEW = DevMenuState(
            build = BuildInfo.PREVIEW,
            session = "ada@example.com",
            offlineSupported = true,
            offline = false,
            realApiSupported = true,
            realApi = false,
            latencyMs = 500,
            failureRate = 10,
            jumps = listOf(
                // This feature's own keys: a preview names no other feature, and the row shows the id.
                ShellJump(id = "gallery", screenId = "GalleryScreen", route = DevMenuDestination),
                ShellJump(id = "tripWizard", screenId = "TripWizardScreen", route = DevMenuPlaygroundDestination()),
                ShellJump(id = "inventoryEditor", screenId = "InventoryEditorScreen", route = DevMenuDestination),
            ),
            sessionShown = true,
            reminderShown = true,
            whatsNewShown = true,
        )
    }
}

/** The states this screen is drawn in: signed in, signed out, and a build with no fixture engine. */
class DevMenuStatePreviews : PreviewParameterProvider<DevMenuState> {

    override val values = sequenceOf(
        DevMenuState.PREVIEW,
        DevMenuState.PREVIEW.copy(
            session = null,
            offline = true,
        ),
        DevMenuState.PREVIEW.copy(
            build = BuildInfo.PREVIEW.copy(
                flavor = "staging",
                baseUrl = "https://staging.example.com/a/rather/long/base/path/",
            ),
            offlineSupported = false,
            realApiSupported = false,
        ),
    )
}
