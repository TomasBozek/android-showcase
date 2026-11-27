package com.example.androidproject1.feature.settings.presentation.settings

import android.Manifest
import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.auth.domain.AppLock
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.onboarding.domain.OnboardingRepository
import com.example.androidproject1.feature.profile.domain.ProfileRepository
import com.example.androidproject1.feature.settings.domain.AppInfo
import com.example.androidproject1.feature.settings.domain.ReviewRepository
import com.example.androidproject1.feature.settings.domain.ThemeRepository
import com.example.androidproject1.feature.settings.presentation.R
import com.example.androidproject1.service.biometric.Availability
import com.example.androidproject1.service.biometric.BiometricGate
import com.example.androidproject1.service.biometric.GateResult
import com.example.androidproject1.service.core.ui.event.ExternalTarget
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.ToastTone
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.device.DeviceInfo
import com.example.androidproject1.service.locale.AppLanguage
import com.example.androidproject1.service.locale.LanguageRepository
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.NotificationPreferences
import com.example.androidproject1.service.permission.PermissionChecker
import com.example.androidproject1.service.permission.PermissionStatus
import kotlinx.coroutines.launch

class SettingsViewModel(
    logger: Logger,
    private val authService: AuthService,
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository,
    private val profileRepository: ProfileRepository,
    private val onboardingRepository: OnboardingRepository,
    private val reviewRepository: ReviewRepository,
    private val appInfo: AppInfo,
    private val deviceInfo: DeviceInfo,
    private val biometric: BiometricGate,
    private val notificationPreferences: NotificationPreferences,
    private val permissions: PermissionChecker,
    private val appLock: AppLock,
) : BaseViewModel<SettingsState, SettingsEvent, SettingsNavigation>(
    initialState = SettingsState(email = null),
    logger = logger.withTag("SettingsViewModel"),
) {

    init {
        // Read once: a screen lock is set in the system's settings, which this screen is not on.
        updateData { copy(appLockAvailable = biometric.availability() == Availability.Ready) }
        viewModelScope.launch {
            appLock.enabled.collect { on -> updateData { copy(appLockOn = on) } }
        }
        // Observed, not read once: the switch follows the store, so what it shows is what the notifier will do.
        viewModelScope.launch {
            notificationPreferences.observe(NotificationKind.Reminders).collect { on ->
                updateData { copy(remindersOn = on) }
            }
        }
        observe(
            flow = { authService.observeSession() },
        ) { session ->
            // updateData, not a fresh SettingsState: `debugMenuEnabled` is reported once by
            // the destination and a rebuilt state would drop it on the next session emission.
            updateData { copy(email = session?.email) }
        }

        // Observed rather than read once: the root applies the same flow, so a choice made
        // here has to come back through the store rather than be held in this state alone.
        observe(
            flow = { themeRepository.observeTheme() },
        ) { theme ->
            updateData { copy(theme = theme) }
        }

        // The account row follows a save on the profile screen, and the Language row
        // shows the stored choice. Silent: a store that cannot be read leaves the row blank,
        // which is what it showed before, and nothing here is worth a dialog over Settings.
        observe(
            flow = { profileRepository.observeProfile() },
            errorDisplay = ErrorDisplay.Silent,
        ) { profile ->
            updateData { copy(profileName = profile.name, avatarUri = profile.avatarUri) }
        }
        observe(
            flow = { languageRepository.observeLanguage() },
            errorDisplay = ErrorDisplay.Silent,
        ) { language ->
            updateData { copy(languageLabel = language.label()) }
        }
    }

    override fun onUiEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.RemindersToggled ->
                viewModelScope.launch { notificationPreferences.set(NotificationKind.Reminders, event.on) }
            is SettingsEvent.AppLockToggled -> viewModelScope.launch { appLock.setEnabled(event.on) }

            is SettingsEvent.PermissionsRead -> {
                val notifications = event.permissions.firstOrNull { it.name == Manifest.permission.POST_NOTIFICATIONS }
                updateData {
                    copy(
                        notificationsRefusedForGood = notifications != null &&
                            permissions.status(notifications) == PermissionStatus.RefusedForGood,
                    )
                }
            }

            SettingsEvent.NotificationSettingsClicked -> sendCommand(UiCommand.OpenAppSettings)

            SettingsEvent.ProfileClicked -> navigate(SettingsNavigation.Profile)
            SettingsEvent.AboutClicked -> navigate(SettingsNavigation.About)
            // Play draws its own sheet, and says nothing about what the user did in it.
            SettingsEvent.RateClicked -> execute(
                errorDisplay = ErrorDisplay.Silent,
                action = { reviewRepository.requestReview() },
                onData = {},
            )
            SettingsEvent.FeedbackClicked -> sendCommand(
                UiCommand.OpenExternal(
                    ExternalTarget.Email(
                        address = appInfo.supportEmail,
                        subject = R.string.settings_feedback_subject.toUiText(),
                        // What a reply needs to know and a user never remembers. Not copy: it is
                        // read by whoever answers, in whatever language the build says.
                        body = "\n\n—\n" + "${deviceInfo.summary} · ${appInfo.flavor} · ${appInfo.buildType}",
                    ),
                ),
            )
            SettingsEvent.HelpClicked -> sendCommand(UiCommand.OpenBrowser(appInfo.helpUrl))
            SettingsEvent.CopyEmailClicked ->
                uiState.value.data?.email
                    ?.takeIf { it.isNotBlank() }
                    ?.let { sendCommand(UiCommand.Copy(it)) }
            // Nothing to navigate to: the flag goes back and the tour starts on the next Home,
            // which is where its first step points. The snackbar is what says it worked.
            SettingsEvent.ReplayTourClicked -> execute(
                action = { onboardingRepository.resetTour() },
                onData = { showSnackbar(message = R.string.settings_tour_replayed.toUiText()) },
            )
            SettingsEvent.PermissionsClicked -> navigate(SettingsNavigation.Permissions)
            SettingsEvent.StorageClicked -> navigate(SettingsNavigation.Storage)
            SettingsEvent.LanguageClicked -> navigate(SettingsNavigation.Language)
            SettingsEvent.DebugMenuClicked -> navigate(SettingsNavigation.DebugMenu)

            is SettingsEvent.DebugMenuAvailable ->
                updateData { copy(debugMenuEnabled = event.available) }

            SettingsEvent.DangerZoneToggled ->
                updateData { copy(dangerZoneExpanded = !dangerZoneExpanded) }

            // No optimistic update: the flow above re-emits, so writing the state here as
            // well would show a choice the store may have refused.
            is SettingsEvent.ThemeSelected -> execute(
                // No overlay: the whole app repaints as soon as the store emits, and a spinner
                // over a repaint the user is already watching says nothing.
                action = { themeRepository.setTheme(event.theme) },
                onData = {},
            )

            SettingsEvent.LogoutClicked -> uiState.setAlert(
                id = ALERT_ID_LOGOUT,
                title = R.string.settings_logout_title.toUiText(),
                message = R.string.settings_logout_confirmation.toUiText(),
                destructive = true,
                confirmLabel = R.string.settings_logout_confirm.toUiText(),
                declineLabel = R.string.settings_logout_cancel.toUiText(),
            )

            // One prompt, biometric or the device credential; a device with no screen lock
            // keeps the confirmation it always had.
            SettingsEvent.DeleteAccountClicked -> viewModelScope.launch {
                when (biometric.confirm(R.string.settings_delete_account_prompt.toUiText())) {
                    GateResult.Confirmed -> deleteAccount()
                    GateResult.Unavailable -> confirmDeletionInDialog()
                    GateResult.Denied -> showToast(R.string.settings_step_up_denied.toUiText(), ToastTone.Error)
                    GateResult.Cancelled -> Unit
                }
            }
        }
    }

    /** Alert results arrive here tagged with the alert's id; delegate what you don't handle. */
    override fun onSystemEvent(event: SystemEvent) {
        when {
            event is SystemEvent.AlertResult &&
                event.id == ALERT_ID_LOGOUT &&
                event is SystemEvent.AlertResult.Confirmed -> {
                super.onSystemEvent(event)
                logout()
            }

            event is SystemEvent.AlertResult &&
                event.id == ALERT_ID_DELETE_ACCOUNT &&
                event is SystemEvent.AlertResult.Confirmed -> {
                super.onSystemEvent(event)
                deleteAccount()
            }

            else -> super.onSystemEvent(event)
        }
    }

    /** The message says what goes and what stays: the account on the server and everything it kept on this device, but not the theme, the language or the tour. */
    private fun confirmDeletionInDialog() = uiState.setAlert(
        id = ALERT_ID_DELETE_ACCOUNT,
        title = R.string.settings_delete_account_title.toUiText(),
        message = R.string.settings_delete_account_message.toUiText(),
        destructive = true,
        confirmLabel = R.string.settings_delete_account_confirm.toUiText(),
        declineLabel = R.string.settings_logout_cancel.toUiText(),
    )

    private fun logout() = execute(
        loading = overlay(),
        action = { authService.logout() },
        onData = { logger.d { "Signed out" } },
    )

    /**
     * Worded, because six databases and a request take a moment and "Loading" over a screen the
     * user just confirmed a deletion on says nothing. A failure is the default alert: the session
     * is still there, and the user can try again.
     */
    private fun deleteAccount() = execute(
        loading = overlay(R.string.settings_deleting.toUiText()),
        action = { authService.deleteAccount() },
        onData = { logger.d { "Account deleted" } },
    )

    /**
     * A language is named in itself in every locale — "English", "Čeština" — so the same strings
     * the picker draws its radios from name the row here; only "follow the device" is translated.
     */
    private fun AppLanguage.label(): UiText = when (this) {
        AppLanguage.System -> R.string.settings_language_system
        AppLanguage.English -> R.string.settings_language_english
        AppLanguage.Czech -> R.string.settings_language_czech
    }.toUiText()

    private companion object {

        const val ALERT_ID_LOGOUT = "logout_confirmation"
        const val ALERT_ID_DELETE_ACCOUNT = "delete_account_confirmation"
    }
}
