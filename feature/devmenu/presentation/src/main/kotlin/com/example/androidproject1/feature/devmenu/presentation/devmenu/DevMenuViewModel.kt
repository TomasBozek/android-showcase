package com.example.androidproject1.feature.devmenu.presentation.devmenu

import androidx.lifecycle.viewModelScope
import com.example.androidproject1.core.ui.shell.ShellJumps
import com.example.androidproject1.feature.devmenu.presentation.DevMenuHooks
import com.example.androidproject1.feature.devmenu.presentation.LogShare
import com.example.androidproject1.feature.devmenu.presentation.MessageReset
import com.example.androidproject1.feature.devmenu.presentation.R
import com.example.androidproject1.feature.devmenu.presentation.ReminderTrigger
import com.example.androidproject1.service.core.domain.error.NetworkError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.event.AppMessage
import com.example.androidproject1.service.core.ui.event.AppMessages
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.ToastTone
import com.example.androidproject1.service.core.ui.form.required
import com.example.androidproject1.service.core.ui.state.AlertAnswer
import com.example.androidproject1.service.core.ui.state.AlertInput
import com.example.androidproject1.service.core.ui.state.AlertOption
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.devtools.ApiSwitch
import com.example.androidproject1.service.devtools.BuildInfo
import com.example.androidproject1.service.devtools.ConfigAnswer
import com.example.androidproject1.service.devtools.NetworkLogBuffer
import com.example.androidproject1.service.devtools.NotificationTester
import com.example.androidproject1.service.devtools.OfflineSwitch
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.play.UpdateKind
import com.example.androidproject1.service.play.UpdateSimulator
import com.example.androidproject1.service.telemetry.CrashLog
import com.example.androidproject1.service.telemetry.CrashReports
import com.example.androidproject1.service.telemetry.ErrorTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * @param buildInfo bound by `:app`, which is the only module allowed to read `BuildConfig`.
 * @param offlineSwitch the fixture engine's flag, or `OfflineSwitch.Unsupported` in a build that
 * talks to a real server.
 * @param apiSwitch the same engine's other flag, or `ApiSwitch.Unsupported`.
 * @param networkLog where the client reports the id of the last request that failed.
 * @param jumps the screens the features contribute to the jump list, read with `getAll()`.
 * @param hooks the rows for features the menu cannot name, each `null` in an app without it.
 */
class DevMenuViewModel(
    logger: Logger,
    buildInfo: BuildInfo,
    private val offlineSwitch: OfflineSwitch,
    private val apiSwitch: ApiSwitch,
    private val notificationTester: NotificationTester,
    private val messageReset: MessageReset,
    private val logShare: LogShare,
    networkLog: NetworkLogBuffer,
    hooks: DevMenuHooks,
    private val errorTracker: ErrorTracker,
    private val messages: AppMessages,
    private val updateSimulator: UpdateSimulator,
    private val crashLog: CrashLog,
    private val crashReports: CrashReports,
    jumps: ShellJumps,
) : BaseViewModel<DevMenuState, DevMenuEvent, DevMenuNavigation>(
    initialState = DevMenuState(
        build = buildInfo,
        session = null,
        offlineSupported = offlineSwitch.isSupported,
        offline = offlineSwitch.isOffline(),
        realApiSupported = apiSwitch.isSupported,
        realApi = apiSwitch.isRealApi(),
        latencyMs = offlineSwitch.latencyMs(),
        failureRate = offlineSwitch.failureRate(),
        sessionExpiryArmed = offlineSwitch.isSessionExpiryArmed(),
        configAnswers = offlineSwitch.configAnswers(),
        fastOrderClock = offlineSwitch.fastOrderClock(),
        pretendGooglePay = offlineSwitch.pretendGooglePay(),
        lastFailedRequestId = networkLog.lastFailedRequestId,
        jumps = jumps.all.sortedBy { it.screenId },
        sessionShown = hooks.session != null,
        reminderShown = hooks.reminder != null,
        whatsNewShown = hooks.whatsNew != null,
    ),
    logger = logger.withTag("DevMenuViewModel"),
) {

    private val sessionReadout = hooks.session
    private val reminderTrigger = hooks.reminder
    private val whatsNewReset = hooks.whatsNew

    init {
        viewModelScope.launch {
            val kept = crashLog.recent().size
            updateData { copy(crashReports = kept) }
        }
        sessionReadout?.let { readout ->
            observe(
                flow = { readout.signedIn().map { Outcome.Success(it) } },
            ) { session ->
                updateData { copy(session = session) }
            }
        }
    }

    private suspend fun fireReminder(trigger: ReminderTrigger): Outcome<UiText> = Outcome.Success(
        when (val fired = trigger.fire()) {
            ReminderTrigger.Fired.NothingToRemind -> R.string.dev_menu_reminder_no_trip.toUiText()
            is ReminderTrigger.Fired.Posted -> R.string.dev_menu_reminder_posted.toUiText(fired.title)
            ReminderTrigger.Fired.Blocked -> R.string.dev_menu_notification_blocked.toUiText()
        },
    )

    override fun onUiEvent(event: DevMenuEvent) {
        when (event) {
            is DevMenuEvent.SessionExpiryToggled -> {
                offlineSwitch.setSessionExpiryArmed(event.armed)
                updateData { copy(sessionExpiryArmed = offlineSwitch.isSessionExpiryArmed()) }
            }

            is DevMenuEvent.FastOrderClockToggled -> {
                offlineSwitch.setFastOrderClock(event.on)
                updateData { copy(fastOrderClock = offlineSwitch.fastOrderClock()) }
            }

            is DevMenuEvent.GooglePayToggled -> {
                offlineSwitch.setPretendGooglePay(event.on)
                updateData { copy(pretendGooglePay = offlineSwitch.pretendGooglePay()) }
            }

            is DevMenuEvent.ConfigToggled -> {
                offlineSwitch.setConfigAnswer(event.answer, event.on)
                updateData { copy(configAnswers = offlineSwitch.configAnswers()) }
            }

            is DevMenuEvent.OfflineToggled -> {
                offlineSwitch.setOffline(event.offline)
                // Read back rather than assumed: the flag lives outside this process, so a write
                // that did not take should not leave the screen claiming it did.
                updateData { copy(offline = offlineSwitch.isOffline()) }
            }

            is DevMenuEvent.RealApiToggled -> {
                apiSwitch.setRealApi(event.realApi)
                updateData { copy(realApi = apiSwitch.isRealApi()) }
            }

            // The two knobs live on the same engine and are read back the same way.
            is DevMenuEvent.LatencyChanged -> {
                offlineSwitch.setLatencyMs(event.latencyMs)
                updateData { copy(latencyMs = offlineSwitch.latencyMs()) }
            }

            is DevMenuEvent.FailureRateChanged -> {
                offlineSwitch.setFailureRate(event.failureRate)
                updateData { copy(failureRate = offlineSwitch.failureRate()) }
            }

            DevMenuEvent.CrashClicked -> {
                // A handled exception, not a thrown one: what is being checked is that reports
                // reach the tracker this build binds, and killing the app proves nothing extra.
                errorTracker.recordNonFatal(
                    DebugMenuTestReport(),
                    "Recorded from the debug menu",
                )
                showToast(R.string.dev_menu_crash_recorded.toUiText())
            }

            // The share sheet is the answer when there is a log; a toast only when there is none.
            DevMenuEvent.ShareCrashReportsClicked -> execute(
                action = { Outcome.Success(crashReports.share()) },
                onData = { shared -> if (!shared) showToast(R.string.dev_menu_crash_reports_none.toUiText()) },
            )

            DevMenuEvent.CrashAppClicked -> throw IllegalStateException("Crash from the debug menu")

            DevMenuEvent.ShareLogsClicked -> execute(
                action = { Outcome.Success(logShare.share()) },
                onData = { shared -> if (!shared) showToast(R.string.dev_menu_share_logs_none.toUiText()) },
            )

            DevMenuEvent.MessageResetClicked -> execute(
                action = {
                    messageReset.reset()
                    Outcome.Success(Unit)
                },
                onData = { showToast(R.string.dev_menu_message_reset_done.toUiText()) },
            )

            // Version 1 rather than 0: 0 is a first run, which shows nothing.
            DevMenuEvent.WhatsNewResetClicked -> whatsNewReset?.let { reset ->
                execute(
                    action = {
                        reset.reset()
                        Outcome.Success(Unit)
                    },
                    onData = { showToast(R.string.dev_menu_whats_new_reset_done.toUiText()) },
                )
            }

            DevMenuEvent.UpdateAvailableClicked -> {
                updateSimulator.makeAvailable(UpdateKind.Flexible)
                showToast(R.string.dev_menu_update_available_done.toUiText())
            }

            DevMenuEvent.UpdateRequiredClicked -> {
                updateSimulator.makeAvailable(UpdateKind.Immediate)
                showToast(R.string.dev_menu_update_required_done.toUiText())
            }

            DevMenuEvent.UpdateDownloadedClicked -> {
                updateSimulator.finishDownload()
                showToast(R.string.dev_menu_update_downloaded_done.toUiText())
            }

            DevMenuEvent.FireReminderClicked -> reminderTrigger?.let { trigger ->
                execute(
                    action = { fireReminder(trigger) },
                    onData = { message -> showToast(message) },
                )
            }

            DevMenuEvent.NotificationClicked -> {
                // A toast either way: a notification that was never posted and one that was
                // posted and missed look identical from here, and only one is a bug.
                val posted = notificationTester.post()
                showToast(
                    if (posted) {
                        R.string.dev_menu_notification_posted.toUiText()
                    } else {
                        R.string.dev_menu_notification_blocked.toUiText()
                    },
                )
            }

            is DevMenuEvent.MessageToastClicked -> showToast(
                R.string.dev_menu_messages_toast_sample.toUiText(event.tone.name),
                tone = event.tone,
            )

            DevMenuEvent.MessageSnackbarClicked -> showSnackbar(
                message = R.string.dev_menu_messages_snackbar_sample.toUiText(),
                actionLabel = R.string.dev_menu_messages_snackbar_action.toUiText(),
                id = SNACKBAR_SAMPLE,
            )

            // Off the main thread and a moment later, so the message can outlive this screen.
            DevMenuEvent.MessageBackgroundClicked -> viewModelScope.launch(Dispatchers.Default) {
                delay(backgroundDelayMillis)
                messages.post(
                    AppMessage(
                        key = "devmenu.background",
                        text = R.string.dev_menu_messages_background_sample.toUiText(),
                        tone = ToastTone.Info,
                    ),
                )
            }

            DevMenuEvent.AskTextClicked -> uiState.setAlert(
                id = ALERT_ASK_TEXT,
                title = R.string.dev_menu_messages_prompt_title.toUiText(),
                message = R.string.dev_menu_messages_prompt_message.toUiText(),
                input = AlertInput.Text(
                    label = R.string.dev_menu_messages_prompt_label.toUiText(),
                    validator = required(),
                ),
            )

            DevMenuEvent.AskChoiceClicked -> askChoice(ALERT_ASK_CHOICE, multiple = false)

            DevMenuEvent.AskChoicesClicked -> askChoice(ALERT_ASK_CHOICES, multiple = true)

            DevMenuEvent.OfflineRetryClicked -> offlineRetry()

            is DevMenuEvent.JumpClicked -> navigate(DevMenuNavigation.Jump(event.jump.route))

            DevMenuEvent.PlaygroundClicked -> navigate(DevMenuNavigation.Playground)
            DevMenuEvent.NetworkLogClicked -> navigate(DevMenuNavigation.NetworkLog)
            DevMenuEvent.NavigateUpClicked -> navigate(DevMenuNavigation.NavigateUp)
        }
    }

    /** How long the background message waits; a test sets it to nothing. */
    internal var backgroundDelayMillis = BACKGROUND_DELAY_MILLIS

    override fun onSystemEvent(event: SystemEvent) {
        when {
            event is SystemEvent.SnackbarAction && event.id == SNACKBAR_SAMPLE ->
                showToast(R.string.dev_menu_messages_snackbar_answered.toUiText(), tone = ToastTone.Success)

            event is SystemEvent.AlertResult.Confirmed && event.answer is AlertAnswer.Text -> {
                super.onSystemEvent(event)
                showToast(R.string.dev_menu_messages_typed.toUiText((event.answer as AlertAnswer.Text).value))
            }

            event is SystemEvent.AlertResult.Confirmed && event.answer is AlertAnswer.Choices -> {
                super.onSystemEvent(event)
                val ids = (event.answer as AlertAnswer.Choices).ids
                showToast(R.string.dev_menu_messages_picked.toUiText(ids.joinToString()))
            }

            else -> super.onSystemEvent(event)
        }
    }

    private fun askChoice(id: String, multiple: Boolean) = uiState.setAlert(
        id = id,
        title = R.string.dev_menu_messages_choice_title.toUiText(),
        message = R.string.dev_menu_messages_choice_message.toUiText(),
        input = AlertInput.Choice(
            options = listOf(
                AlertOption("tea", R.string.dev_menu_messages_choice_tea.toUiText()),
                AlertOption("coffee", R.string.dev_menu_messages_choice_coffee.toUiText()),
                AlertOption("cake", R.string.dev_menu_messages_choice_cake.toUiText()),
            ),
            multiple = multiple,
        ),
    )

    /**
     * The fixture engine goes offline and a call fails on it, so the offline alert comes up with
     * its retry armed; the switch comes back a moment later, and the call runs again by
     * itself — which is the whole thing to see.
     */
    private fun offlineRetry() {
        offlineSwitch.setOffline(true)
        updateData { copy(offline = offlineSwitch.isOffline()) }
        execute(
            action = {
                if (offlineSwitch.isOffline()) Outcome.Failure(NetworkError()) else Outcome.Success(Unit)
            },
            onData = { showToast(R.string.dev_menu_messages_offline_done.toUiText(), tone = ToastTone.Success) },
        )
        viewModelScope.launch {
            delay(backgroundDelayMillis)
            offlineSwitch.setOffline(false)
            updateData { copy(offline = offlineSwitch.isOffline()) }
        }
    }

    private companion object {
        const val SNACKBAR_SAMPLE = "devmenu_snackbar"
        const val ALERT_ASK_TEXT = "devmenu_ask_text"
        const val ALERT_ASK_CHOICE = "devmenu_ask_choice"
        const val ALERT_ASK_CHOICES = "devmenu_ask_choices"
        const val BACKGROUND_DELAY_MILLIS = 3_000L
    }
}

/** Its own type so the report is recognisable in whatever console the build reports to. */
class DebugMenuTestReport : RuntimeException("Test report from the debug menu")

/** Every startup-config answer the switch has on, read back rather than assumed. */
private fun OfflineSwitch.configAnswers(): Set<ConfigAnswer> = ConfigAnswer.entries.filter(::configAnswer).toSet()
