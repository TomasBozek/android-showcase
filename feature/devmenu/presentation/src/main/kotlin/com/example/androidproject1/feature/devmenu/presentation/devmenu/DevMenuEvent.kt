package com.example.androidproject1.feature.devmenu.presentation.devmenu

import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.service.core.ui.event.ToastTone
import com.example.androidproject1.service.core.ui.event.UiEvent
import com.example.androidproject1.service.devtools.ConfigAnswer

sealed interface DevMenuEvent : UiEvent {

    /** The fixture engine's "server is down" switch — see `OfflineSwitch`. */
    data class OfflineToggled(val offline: Boolean) : DevMenuEvent

    /** Arm the fixture's one-shot `401`, to see the session end. */
    data class SessionExpiryToggled(val armed: Boolean) : DevMenuEvent

    data class FastOrderClockToggled(val on: Boolean) : DevMenuEvent

    /** "Pretend Google Pay is ready": the payment step's button shows, and a payment answers with a fake token. */
    data class GooglePayToggled(val on: Boolean) : DevMenuEvent

    data class ConfigToggled(val answer: ConfigAnswer, val on: Boolean) : DevMenuEvent

    /** The fixture engine's "send requests to the real hosts" switch — see `ApiSwitch`. */
    data class RealApiToggled(val realApi: Boolean) : DevMenuEvent

    /** The latency stepper moved, in milliseconds. */
    data class LatencyChanged(val latencyMs: Int) : DevMenuEvent

    /** The failure-rate segment changed, in percent. */
    data class FailureRateChanged(val failureRate: Int) : DevMenuEvent

    /** Sends a handled exception to whatever `ErrorTracker` this build binds. */
    data object CrashClicked : DevMenuEvent

    /**
     * Posts the trip reminder for the soonest upcoming trip **now**, whatever its switch says, so
     * one can be seen without waiting for 18:00 the evening before.
     */
    data object FireReminderClicked : DevMenuEvent

    /**
     * Puts a flexible Play update on offer, in the fake manager `dev` runs on: with the
     * config's "newer version" switch on, the next start downloads it in the background.
     */
    data object UpdateAvailableClicked : DevMenuEvent

    /** Puts an immediate update on offer: what the hold screen's Update starts in place of the store page. */
    data object UpdateRequiredClicked : DevMenuEvent

    /** Finishes the download the flexible update started, so the Restart snackbar shows. */
    data object UpdateDownloadedClicked : DevMenuEvent

    /** Posts a notification whose tap opens a product deep link — see `NotificationTester`. */
    data object NotificationClicked : DevMenuEvent

    /** Shares the log file this build keeps, through the share sheet — see `LogShare`. */
    data object ShareLogsClicked : DevMenuEvent

    /** Shares the crash and ANR records the phone keeps, through the share sheet. */
    data object ShareCrashReportsClicked : DevMenuEvent

    /** Throws on the main thread, so the crash record and the next launch's offer can be walked. */
    data object CrashAppClicked : DevMenuEvent

    /** Forget the dismissed server messages — see `MessageReset`. */
    data object MessageResetClicked : DevMenuEvent

    /** Make the next start show What's new, as after an update. */
    data object WhatsNewResetClicked : DevMenuEvent

    /** A row of the jump list: a screen another feature contributed. */
    data class JumpClicked(val jump: ShellJump) : DevMenuEvent

    /** A toast in [tone] — the messages group. */
    data class MessageToastClicked(val tone: ToastTone) : DevMenuEvent

    /** A snackbar with an action, answered when it is pressed. */
    data object MessageSnackbarClicked : DevMenuEvent

    /** A message posted to the outlet from a background coroutine, a moment later. */
    data object MessageBackgroundClicked : DevMenuEvent

    /** A dialog that asks for a word. */
    data object AskTextClicked : DevMenuEvent

    /** A dialog that asks for one choice. */
    data object AskChoiceClicked : DevMenuEvent

    /** A dialog that asks for several. */
    data object AskChoicesClicked : DevMenuEvent

    /** Goes offline, fails a call, and comes back, so the armed retry runs by itself. */
    data object OfflineRetryClicked : DevMenuEvent

    /** The component playground, a screen of this feature. */
    data object PlaygroundClicked : DevMenuEvent

    data object NetworkLogClicked : DevMenuEvent

    data object NavigateUpClicked : DevMenuEvent
}
