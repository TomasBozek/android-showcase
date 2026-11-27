package com.example.androidproject1.feature.devmenu.presentation.devmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.choice.AppSegmented
import com.example.androidproject1.core.ui.component.choice.AppStepper
import com.example.androidproject1.core.ui.component.choice.AppSwitch
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppDescriptionList
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.list.DescriptionRow
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.devmenu.presentation.R
import com.example.androidproject1.service.core.ui.event.ToastTone
import com.example.androidproject1.service.devtools.ConfigAnswer

/**
 * What a tester needs and nobody else should see: which build this is, what it talks to, who is
 * signed in, a jump to any screen worth reaching without walking the app to it, and the two
 * switches that are otherwise a `touch` over adb.
 *
 * Only `dev` and `staging` reach it — see `DebugMenu.ENABLED` in `:app`'s flavor source sets.
 */
@Composable
fun DevMenuScreen(
    state: DevMenuState,
    onEvent: (DevMenuEvent) -> Unit,
) {
    AppScaffold(
        screenId = "DevMenuScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.dev_menu_title),
                onNavigateUp = { onEvent(DevMenuEvent.NavigateUpClicked) },
                navigateUpTestTag = "devMenu_upButton",
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            AppSectionHeader(title = stringResource(R.string.dev_menu_build))
            AppDescriptionList(
                modifier = Modifier.testTag("devMenu_buildList"),
                rows = listOf(
                    DescriptionRow(
                        label = stringResource(R.string.dev_menu_flavor),
                        value = "${state.build.flavor} · ${state.build.buildType}",
                    ),
                    DescriptionRow(
                        label = stringResource(R.string.dev_menu_application_id),
                        value = state.build.applicationId,
                    ),
                    DescriptionRow(
                        label = stringResource(R.string.dev_menu_version),
                        value = "${state.build.versionName} (${state.build.versionCode})",
                    ),
                    DescriptionRow(
                        label = stringResource(R.string.dev_menu_base_url),
                        value = state.build.baseUrl,
                    ),
                    DescriptionRow(
                        label = stringResource(R.string.dev_menu_leaks),
                        value = stringResource(
                            if (state.build.leakDetection) {
                                R.string.dev_menu_leaks_on
                            } else {
                                R.string.dev_menu_leaks_off
                            },
                        ),
                    ),
                ),
            )

            if (state.sessionShown) {
                AppSectionHeader(title = stringResource(R.string.dev_menu_session))
                AppText(
                    // Never the id: an address is what identifies the tester's own account, and the
                    // id is the only part that may leave the app.
                    text = state.session ?: stringResource(R.string.dev_menu_session_none),
                    role = TextRole.BodyLarge,
                    modifier = Modifier.testTag("devMenu_sessionValue"),
                )
            }

            AppSectionHeader(title = stringResource(R.string.dev_menu_last_request))
            AppText(
                text = state.lastFailedRequestId ?: stringResource(R.string.dev_menu_last_request_none),
                role = TextRole.BodyLarge,
                modifier = Modifier.testTag("devMenu_lastRequestValue"),
            )

            AppSectionHeader(title = stringResource(R.string.dev_menu_jumps))
            // Its own column: the outer one spaces sections apart, and rows of a list sit flush.
            Column {
                state.jumps.forEach { jump ->
                    AppListItem(
                        headline = jump.screenId,
                        onClick = { onEvent(DevMenuEvent.JumpClicked(jump)) },
                        modifier = Modifier.testTag("devMenu_${jump.id}Item"),
                    )
                }
            }

            if (state.offlineSupported) {
                AppSectionHeader(title = stringResource(R.string.dev_menu_network))
                AppSwitch(
                    checked = state.offline,
                    onCheckedChange = { onEvent(DevMenuEvent.OfflineToggled(it)) },
                    label = stringResource(R.string.dev_menu_offline),
                    supporting = stringResource(R.string.dev_menu_offline_supporting),
                    modifier = Modifier.testTag("devMenu_offlineSwitch"),
                )
                AppSwitch(
                    checked = state.sessionExpiryArmed,
                    onCheckedChange = { onEvent(DevMenuEvent.SessionExpiryToggled(it)) },
                    label = stringResource(R.string.dev_menu_session_expired),
                    supporting = stringResource(R.string.dev_menu_session_expired_supporting),
                    modifier = Modifier.testTag("devMenu_sessionExpiredSwitch"),
                )
                AppSwitch(
                    checked = state.fastOrderClock,
                    onCheckedChange = { onEvent(DevMenuEvent.FastOrderClockToggled(it)) },
                    label = stringResource(R.string.dev_menu_order_clock),
                    supporting = stringResource(R.string.dev_menu_order_clock_supporting),
                    modifier = Modifier.testTag("devMenu_orderClockSwitch"),
                )
                AppSwitch(
                    checked = state.pretendGooglePay,
                    onCheckedChange = { onEvent(DevMenuEvent.GooglePayToggled(it)) },
                    label = stringResource(R.string.dev_menu_google_pay),
                    supporting = stringResource(R.string.dev_menu_google_pay_supporting),
                    modifier = Modifier.testTag("devMenu_googlePaySwitch"),
                )
                // The startup config's three answers, served at the next start.
                ConfigAnswer.entries.forEach { answer ->
                    AppSwitch(
                        checked = answer in state.configAnswers,
                        onCheckedChange = { onEvent(DevMenuEvent.ConfigToggled(answer, it)) },
                        label = stringResource(answer.label),
                        supporting = stringResource(R.string.dev_menu_config_supporting),
                        modifier = Modifier.testTag(answer.testTag),
                    )
                }
                // The engine's two knobs: a wait before every answer, in 250 ms steps,
                // and a share of answers that fail — seeded, so a run repeats.
                AppListItem(
                    headline = stringResource(R.string.dev_menu_latency),
                    supporting = stringResource(R.string.dev_menu_latency_supporting),
                    trailing = {
                        AppStepper(
                            value = state.latencyMs,
                            onValueChange = { onEvent(DevMenuEvent.LatencyChanged(it)) },
                            min = 0,
                            max = MAX_LATENCY_MS,
                            step = LATENCY_STEP_MS,
                            modifier = Modifier.testTag("devMenu_latencyField"),
                        )
                    },
                )
                AppListItem(
                    headline = stringResource(R.string.dev_menu_failure_rate),
                    supporting = stringResource(R.string.dev_menu_failure_rate_supporting),
                )
                AppSegmented(
                    options = FAILURE_RATES.map { stringResource(R.string.dev_menu_failure_rate_option, it) },
                    selectedIndex = FAILURE_RATES.indexOf(state.failureRate).coerceAtLeast(0),
                    onSelect = { onEvent(DevMenuEvent.FailureRateChanged(FAILURE_RATES[it])) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("devMenu_failureRateField"),
                )
            }
            if (state.realApiSupported) {
                if (!state.offlineSupported) AppSectionHeader(title = stringResource(R.string.dev_menu_network))
                AppSwitch(
                    checked = state.realApi,
                    onCheckedChange = { onEvent(DevMenuEvent.RealApiToggled(it)) },
                    label = stringResource(R.string.dev_menu_real_api),
                    supporting = stringResource(R.string.dev_menu_real_api_supporting),
                    modifier = Modifier.testTag("devMenu_realApiSwitch"),
                )
            }

            // Every kind of message and question, so a tester sees each without a screen
            // that raises it — the rules are DESIGN-SYSTEM.md § Telling the user.
            AppSectionHeader(title = stringResource(R.string.dev_menu_messages))
            ToastTone.entries.forEach { tone ->
                AppButton(
                    label = stringResource(R.string.dev_menu_messages_toast, tone.name),
                    onClick = { onEvent(DevMenuEvent.MessageToastClicked(tone)) },
                    kind = ButtonKind.Outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("devMenu_toast${tone.name}Button"),
                )
            }
            listOf(
                Triple(
                    R.string.dev_menu_messages_snackbar,
                    DevMenuEvent.MessageSnackbarClicked,
                    "devMenu_snackbarButton",
                ),
                Triple(
                    R.string.dev_menu_messages_background,
                    DevMenuEvent.MessageBackgroundClicked,
                    "devMenu_backgroundButton",
                ),
                Triple(R.string.dev_menu_messages_prompt, DevMenuEvent.AskTextClicked, "devMenu_promptButton"),
                Triple(R.string.dev_menu_messages_choice, DevMenuEvent.AskChoiceClicked, "devMenu_choiceButton"),
                Triple(R.string.dev_menu_messages_choices, DevMenuEvent.AskChoicesClicked, "devMenu_choicesButton"),
                Triple(
                    R.string.dev_menu_messages_offline,
                    DevMenuEvent.OfflineRetryClicked,
                    "devMenu_offlineRetryButton",
                ),
            ).forEach { (label, event, tag) ->
                AppButton(
                    label = stringResource(label),
                    onClick = { onEvent(event) },
                    kind = ButtonKind.Outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag),
                )
            }

            AppSectionHeader(title = stringResource(R.string.dev_menu_tools))
            // Every exchange the client made since start, with both bodies.
            AppButton(
                label = stringResource(R.string.dev_menu_network_log),
                onClick = { onEvent(DevMenuEvent.NetworkLogClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_networkLogButton"),
            )
            // The component bench: this feature's own screen, so a push rather than a jump.
            AppButton(
                label = stringResource(R.string.dev_menu_playground),
                onClick = { onEvent(DevMenuEvent.PlaygroundClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_playgroundButton"),
            )
            AppButton(
                label = stringResource(R.string.dev_menu_notification),
                onClick = { onEvent(DevMenuEvent.NotificationClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_notificationButton"),
            )
            if (state.reminderShown) {
                AppButton(
                    label = stringResource(R.string.dev_menu_fire_reminder),
                    onClick = { onEvent(DevMenuEvent.FireReminderClicked) },
                    kind = ButtonKind.Outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("devMenu_fireReminderButton"),
                )
            }
            AppButton(
                label = stringResource(R.string.dev_menu_update_available),
                onClick = { onEvent(DevMenuEvent.UpdateAvailableClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_updateAvailableButton"),
            )
            AppButton(
                label = stringResource(R.string.dev_menu_update_required),
                onClick = { onEvent(DevMenuEvent.UpdateRequiredClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_updateRequiredButton"),
            )
            AppButton(
                label = stringResource(R.string.dev_menu_update_downloaded),
                onClick = { onEvent(DevMenuEvent.UpdateDownloadedClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_updateDownloadedButton"),
            )
            AppButton(
                label = stringResource(R.string.dev_menu_message_reset),
                onClick = { onEvent(DevMenuEvent.MessageResetClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_messageResetButton"),
            )
            if (state.whatsNewShown) {
                AppButton(
                    label = stringResource(R.string.dev_menu_whats_new_reset),
                    onClick = { onEvent(DevMenuEvent.WhatsNewResetClicked) },
                    kind = ButtonKind.Outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("devMenu_whatsNewResetButton"),
                )
            }
            AppButton(
                label = stringResource(R.string.dev_menu_share_logs),
                onClick = { onEvent(DevMenuEvent.ShareLogsClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_shareLogsButton"),
            )
            AppButton(
                label = pluralStringResource(R.plurals.dev_menu_crash_reports, state.crashReports, state.crashReports),
                onClick = { onEvent(DevMenuEvent.ShareCrashReportsClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_crashReportsButton"),
            )
            AppButton(
                label = stringResource(R.string.dev_menu_crash_app),
                onClick = { onEvent(DevMenuEvent.CrashAppClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_crashAppButton"),
            )
            AppButton(
                label = stringResource(R.string.dev_menu_crash),
                onClick = { onEvent(DevMenuEvent.CrashClicked) },
                kind = ButtonKind.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("devMenu_crashButton"),
            )
        }
    }
}

@ScreenPreview
@Composable
private fun Preview(
    @PreviewParameter(DevMenuStatePreviews::class) state: DevMenuState,
) = ThemedScreenPreview {
    DevMenuScreen(
        state = state,
    ) {}
}

/** The stepper's unit and ceiling, and the three rates worth a segment each. */
private const val LATENCY_STEP_MS = 250
private const val MAX_LATENCY_MS = 5_000
private val FAILURE_RATES = listOf(0, 10, 50)

private val ConfigAnswer.label: Int
    get() = when (this) {
        ConfigAnswer.BelowMinimum -> R.string.dev_menu_config_below_minimum
        ConfigAnswer.Maintenance -> R.string.dev_menu_config_maintenance
        ConfigAnswer.NewerAvailable -> R.string.dev_menu_config_newer_available
    }

private val ConfigAnswer.testTag: String
    get() = when (this) {
        ConfigAnswer.BelowMinimum -> "devMenu_belowMinimumSwitch"
        ConfigAnswer.Maintenance -> "devMenu_maintenanceSwitch"
        ConfigAnswer.NewerAvailable -> "devMenu_newerVersionSwitch"
    }
