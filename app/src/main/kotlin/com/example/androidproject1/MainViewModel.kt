package com.example.androidproject1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.config.AppConfigRepository
import com.example.androidproject1.config.AppHold
import com.example.androidproject1.config.holdFor
import com.example.androidproject1.feature.auth.domain.AppLock
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.domain.TermsPolicy
import com.example.androidproject1.feature.onboarding.domain.OnboardingRepository
import com.example.androidproject1.feature.settings.domain.ThemePreference
import com.example.androidproject1.feature.settings.domain.ThemeRepository
import com.example.androidproject1.message.MessageRepository
import com.example.androidproject1.message.ServerMessage
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.event.AppMessage
import com.example.androidproject1.service.core.ui.event.AppMessages
import com.example.androidproject1.service.core.ui.event.MessageAction
import com.example.androidproject1.service.core.ui.event.MessageDuration
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import com.example.androidproject1.service.device.DeviceInfo
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.navigation.AppNavigation
import com.example.androidproject1.service.navigation.AppNavigatorChannel
import com.example.androidproject1.service.network.ConnectivityMonitor
import com.example.androidproject1.service.play.AppUpdates
import com.example.androidproject1.service.play.UpdateKind
import com.example.androidproject1.service.session.SessionExpiry
import com.example.androidproject1.service.telemetry.CrashLog
import com.example.androidproject1.service.telemetry.ErrorTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import com.example.androidproject1.service.core.ui.R as CoreR

/**
 * Single owner of the session, the first-run flag and the stored theme, and the only thing that
 * switches between the app's three flows.
 *
 * A plain [ViewModel] rather than a `BaseViewModel`, because it is not a screen: there is no state
 * to render, no user event to receive, and nothing to put a loading overlay or an error dialog
 * over. Screens change what is stored and let this react — see `SettingsViewModel.logout()` and
 * `OnboardingViewModel.finish()`.
 */
class MainViewModel(
    logger: Logger,
    private val authService: AuthService,
    private val onboardingRepository: OnboardingRepository,
    private val themeRepository: ThemeRepository,
    private val errorTracker: ErrorTracker,
    connectivity: ConnectivityMonitor,
    private val sessionExpiry: SessionExpiry,
    private val termsPolicy: TermsPolicy,
    private val messageRepository: MessageRepository,
    private val deviceInfo: DeviceInfo,
    private val clock: Clock,
    private val appConfigRepository: AppConfigRepository,
    private val appMessages: AppMessages,
    private val appNavigator: AppNavigatorChannel,
    private val appUpdates: AppUpdates,
    private val appLock: AppLock,
    private val crashLog: CrashLog,
) : ViewModel() {

    private val logger = logger.withTag(TAG)

    /**
     * Whether the whole app is held behind one screen: a build below the server's minimum,
     * or maintenance. Independent of the session — a signed-out user is held too — and `null`
     * while the config is unknown, because the config fails open.
     */
    val hold: StateFlow<AppHold?> = appConfigRepository.config
        .map { it?.holdFor(deviceInfo.versionCode) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val dismissedUpdate = MutableStateFlow<Int?>(null)

    /**
     * A newer build to offer in Home's header: the latest version, when it is above this
     * one and not the one whose offer was dismissed. `null` offers nothing — and is what a held app
     * gets too, since the hold screen is all it draws.
     */
    val updateOffer: StateFlow<Int?> = combine(appConfigRepository.config, dismissedUpdate) { config, dismissed ->
        config?.latestVersionCode?.takeIf { dismissed != null && it > deviceInfo.versionCode && it > dismissed }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Once per start: what the server says now, or what it said last.
        viewModelScope.launch {
            dismissedUpdate.value = appConfigRepository.dismissedUpdate()
            appConfigRepository.refresh()
        }
    }

    init {
        observeFlexibleUpdate()
        offerCrashReport()
    }

    /**
     * Once per crash: the launch after one picks up what the system recorded of an ANR or a
     * native crash, then says so over whatever screen is up and offers to mail the records to
     * support. Nothing is sent unless the user presses it and sends the mail.
     */
    private fun offerCrashReport() {
        viewModelScope.launch {
            crashLog.collect()
            crashLog.takeNew() ?: return@launch
            appMessages.post(
                AppMessage(
                    key = CRASHED_MESSAGE,
                    text = R.string.main_crashed.toUiText(),
                    action = MessageAction.Handler(
                        label = R.string.main_crashed_send.toUiText(),
                        handlerKey = CrashReportHandler.KEY,
                        argument = "",
                    ),
                    duration = MessageDuration.Indefinite,
                ),
            )
        }
    }

    /**
     * The update that does not stop anyone: when the server says a newer build exists and
     * the app is not held for a stricter reason, ask Play for it in the background; when it has
     * downloaded, a snackbar offers Restart — through the outlet, so it is said over whatever
     * screen is up, and its action still works if that screen has gone.
     */
    private fun observeFlexibleUpdate() {
        viewModelScope.launch {
            appConfigRepository.config.filterNotNull().first { it.latestVersionCode > deviceInfo.versionCode }
            checkForUpdate()
        }
        viewModelScope.launch {
            appUpdates.downloaded.collect {
                appMessages.post(
                    AppMessage(
                        key = UPDATE_DOWNLOADED_MESSAGE,
                        text = R.string.main_update_ready.toUiText(),
                        action = MessageAction.Handler(
                            label = R.string.main_update_restart.toUiText(),
                            handlerKey = UpdateRestartHandler.KEY,
                            argument = "",
                        ),
                        priority = AppMessage.Priority.High,
                        duration = MessageDuration.Indefinite,
                    ),
                )
            }
        }
    }

    private var flexibleAsked = false

    /**
     * Asks Play for the newer build the server named, once: the activity calls it on every resume
     * (Play's own advice, so an update in progress is picked up again), and it does nothing when
     * there is no newer build, the app is held, or Play has nothing for this device.
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            val config = appConfigRepository.config.value ?: return@launch
            if (flexibleAsked || config.latestVersionCode <= deviceInfo.versionCode) return@launch
            if (config.holdFor(deviceInfo.versionCode) != null) return@launch
            val offer = appUpdates.check() ?: return@launch
            if (!offer.flexibleAllowed) return@launch
            flexibleAsked = true
            appUpdates.start(UpdateKind.Flexible)
        }
    }

    /** Puts the offer away, for this version and every older one. */
    fun dismissUpdate() {
        val offered = updateOffer.value ?: return
        dismissedUpdate.value = offered
        viewModelScope.launch { appConfigRepository.dismissUpdate(offered) }
    }

    // `null` means "not read yet", which is what makes the combination below say Unknown until
    // both stores have answered. Two flows rather than one combine over the sources themselves,
    // so a re-emission of the flag does not re-report the user to the crash tracker.
    private val signedIn = MutableStateFlow<Boolean?>(null)
    private val onboardingSeen = MutableStateFlow<Boolean?>(null)

    /** [SessionState.Unknown] until both the stored session and the first-run flag have been read. */
    val sessionState: StateFlow<SessionState> =
        combine(signedIn, onboardingSeen, ::flowFor)
            .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Unknown)

    /**
     * Whether the app lock holds the signed-in app: `MainActivity` draws the lock screen
     * over the main flow while `true`, and keeps the splash up while `null` — the lock has not read
     * its switch yet, and a guess would flash one screen or the other. Never held while signed out.
     */
    val locked: StateFlow<Boolean?> =
        combine(sessionState, appLock.locked) { session, held ->
            when (session) {
                SessionState.Unknown -> null
                SessionState.SignedIn -> held
                else -> false
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The terms version the signed-in account accepted, `null` while nobody is signed in. */
    private val acceptedTerms = MutableStateFlow<Int?>(null)

    /**
     * Whether the signed-in account accepted terms older than this build's. `MainActivity`
     * asks, once, over whatever screen is up: [acceptTerms] stores the new version, [declineTerms]
     * signs out. A plain `ViewModel` has no alert of its own, so the question is state the
     * activity draws rather than an `AlertPayload`.
     */
    val termsOutdated: StateFlow<Boolean> =
        combine(sessionState, acceptedTerms) { session, accepted ->
            session == SessionState.SignedIn && accepted != null && accepted < termsPolicy.version
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun acceptTerms() {
        viewModelScope.launch { authService.acceptTerms(termsPolicy.version) }
    }

    fun declineTerms() {
        viewModelScope.launch { authService.logout() }
    }

    private val mutableWhatsNew = MutableStateFlow(false)

    /**
     * Whether this start follows an update: the stored `versionCode` is below this build's.
     * `MainActivity` shows the What's new sheet over whichever flow is up, and [whatsNewSeen]
     * stores the code, so it is shown once per update. A first run stores the code and shows
     * nothing — there is nothing new to someone who has seen nothing.
     */
    val whatsNew: StateFlow<Boolean> = mutableWhatsNew.asStateFlow()

    fun whatsNewSeen() {
        mutableWhatsNew.value = false
        viewModelScope.launch { onboardingRepository.setLastSeenVersionCode(deviceInfo.versionCode) }
    }

    private val mutableMessage = MutableStateFlow<ServerMessage?>(null)

    /**
     * The server's message to show, if any: fetched once per start, after the first
     * sign-in, and only one that is live and has not been dismissed on this install. Home shows a
     * banner in its header, `MainActivity` a dialog; both put it away through [dismissMessage] or
     * [followMessage].
     */
    val message: StateFlow<ServerMessage?> = mutableMessage.asStateFlow()

    /** Puts the message away for good: its id is stored, and it is never shown again. */
    fun dismissMessage() {
        val id = mutableMessage.value?.id ?: return
        mutableMessage.value = null
        viewModelScope.launch { messageRepository.dismiss(id) }
    }

    /** The message's action: its link is followed like any other, and the message is done with. */
    fun followMessage() {
        // Through the app navigator like any code that is not a screen: it lands after the
        // session gate, the way a link from outside does.
        mutableMessage.value?.action?.let(appNavigator::open)
        dismissMessage()
    }

    /**
     * What code outside a screen asked for, applied the way a warm deep link is: after the
     * session gate, a tab root selected and anything else pushed.
     */
    private fun followAppNavigation() {
        viewModelScope.launch {
            appNavigator.requests.collect { request ->
                when (request) {
                    is AppNavigation.Keys -> if (request.keys.isNotEmpty()) {
                        pendingNeedsSession = true
                        mutableDeepLink.value = request.keys
                    }
                    is AppNavigation.Link -> onDeepLink(request.deepLink, coldStart = false)
                }
            }
        }
    }

    private val mutableDeepLink = MutableStateFlow<List<NavKey>>(emptyList())

    /**
     * The keys a link asked for, waiting to be applied to the back stack.
     *
     * Empty when there is nothing pending. `MainActivity` applies it and calls
     * [onDeepLinkApplied] — a one-shot handed over as state rather than as an event, because a
     * link that arrives before the flow is known has to survive until there is a stack to put
     * it on.
     */
    val deepLink: StateFlow<List<NavKey>> = mutableDeepLink.asStateFlow()

    private val mutableTheme = MutableStateFlow<ThemePreference?>(null)

    /**
     * The palette the whole app draws in, `null` until the stored choice has been read once.
     *
     * Nullable for the same reason [SessionState.Unknown] exists: a default applied before the
     * read lands is a light frame in front of someone who chose dark. The splash holds until
     * this is known, so nothing is drawn in the wrong palette and then swapped.
     */
    val theme: StateFlow<ThemePreference?> = mutableTheme.asStateFlow()

    /**
     * Whether the device has a route to the outside world, for the banner `MainActivity` draws
     * above every screen. The monitor `:app` binds per flavor owns it; it is exposed here so
     * the activity has one view model to ask.
     */
    val online: StateFlow<Boolean> = connectivity.online

    init {
        observeSession()
        observeExpiry()
        followAppNavigation()
        fetchMessage()
        checkWhatsNew()
        observeOnboarding()
        observeTheme()
    }

    // A member function, not an `init` body: a constructor parameter shadows the property of the
    // same name inside `init`, so `logger` there would be the untagged one.
    /**
     * Turns an incoming link into the keys it opens onto.
     *
     * @param coldStart whether the app was launched by this link. On a cold start Home goes under
     * the path the feature's table gives — its tab, then the target — so Up walks back
     * through the app instead of closing it. On a warm one only the target is
     * handed over: a tab root is selected, anything else is pushed onto whatever the user was on.
     */
    fun onDeepLink(uri: String?, coldStart: Boolean) {
        val target = DeepLinks.parse(uri) ?: return
        viewModelScope.launch {
            pendingNeedsSession = target.needsSession
            mutableDeepLink.value = keysFor(target, coldStart)
        }
    }

    /** Whether the pending link waits for a session; a reset link lands on whatever is up. */
    private var pendingNeedsSession = true

    /** The pending link is one followed by someone who cannot sign in, so it need not wait. */
    val deepLinkLandsWithoutSession: Boolean get() = !pendingNeedsSession

    /**
     * What a feature's target becomes on the stack: Home under the whole path on a cold
     * start, the target alone on a warm one. A link that needs no session lands as it is, cold or
     * warm — the flow up is Login when signed out, and a synthesised path would replace that root
     * with a signed-in one.
     */
    private fun keysFor(target: DeepLinkTarget, coldStart: Boolean): List<NavKey> {
        val keys = target.keys
        return when {
            !target.needsSession -> keys
            !coldStart -> listOf(keys.last())
            else -> (listOf(TopLevelDestination.Home.key) + keys).distinct()
        }
    }

    /** Called once the keys are on the back stack, so a rotation does not apply them again. */
    fun onDeepLinkApplied() {
        mutableDeepLink.value = emptyList()
    }

    private fun observeSession() {
        viewModelScope.launch {
            authService.observeSession().collect { outcome ->
                signedIn.value = when (outcome) {
                    is Outcome.Success -> {
                        // The only place that knows who is signed in, so the only place that can
                        // tell the tracker. The id is opaque — never the address, because a crash
                        // report is not the place for one — and null on sign-out, or the next
                        // person's reports are attributed to the last one.
                        errorTracker.setUser(outcome.data?.id)
                        acceptedTerms.value = outcome.data?.termsVersion
                        outcome.data != null
                    }

                    // `observeSession()` has already retried, and there is no screen to put a
                    // dialog over. Signed out is the safe reading and the one flow from which the
                    // user can do something about it.
                    is Outcome.Failure -> {
                        logger.w { "Session unreadable (${outcome.error}); treating as signed out" }
                        errorTracker.setUser(null)
                        false
                    }
                }
            }
        }
    }

    /**
     * A `401` from any request ends the session here, once: the session's owner is the one
     * place that may sign out, and the flow switches to Login by itself when it does. A signal
     * while nobody is signed in — a stray request finishing after a sign-out — is not news.
     */
    private fun observeExpiry() {
        viewModelScope.launch {
            sessionExpiry.signals.collect {
                if (sessionState.value != SessionState.SignedIn) return@collect
                logger.w { "Session expired; signing out" }
                // Said through the outlet, so whichever screen is up says it — Login, once
                // the flow switches — rather than a mark the sign-in screen reads on opening.
                if (authService.logout() is Outcome.Success) {
                    appMessages.post(
                        AppMessage(
                            key = SESSION_EXPIRED_MESSAGE,
                            text = CoreR.string.core_error_session_expired.toUiText(),
                            priority = AppMessage.Priority.High,
                            duration = MessageDuration.Long,
                        ),
                    )
                }
            }
        }
    }

    /**
     * Once per start, once someone is signed in: a message is for a user, and a signed-out app
     * has none to show. A failed fetch means no message — never a dialog about a message.
     */
    private fun fetchMessage() {
        viewModelScope.launch {
            sessionState.first { it == SessionState.SignedIn }
            val dismissed = runCatching { messageRepository.dismissed() }.getOrDefault(emptySet())
            val now = clock.instant()
            mutableMessage.value = runCatching { messageRepository.messages() }
                .onFailure { logger.w { "Messages unavailable ($it); showing none" } }
                .getOrDefault(emptyList())
                .firstOrNull { it.id !in dismissed && it.isLive(now) }
        }
    }

    private fun checkWhatsNew() {
        viewModelScope.launch {
            // Unreadable reads as nothing to show: a sheet is not worth a wrong guess.
            val last = (onboardingRepository.lastSeenVersionCode() as? Outcome.Success)?.data ?: return@launch
            when {
                last == 0 -> onboardingRepository.setLastSeenVersionCode(deviceInfo.versionCode)
                last < deviceInfo.versionCode -> mutableWhatsNew.value = true
            }
        }
    }

    private fun observeOnboarding() {
        viewModelScope.launch {
            onboardingRepository.observeSeen().collect { outcome ->
                onboardingSeen.value = when (outcome) {
                    is Outcome.Success -> outcome.data

                    // Seen, on an unreadable flag: showing the tour to someone who has already
                    // taken it every time the disk hiccups is worse than skipping it once.
                    is Outcome.Failure -> {
                        logger.w { "Onboarding flag unreadable (${outcome.error}); skipping it" }
                        true
                    }
                }
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            themeRepository.observeTheme().collect { outcome ->
                mutableTheme.value = when (outcome) {
                    is Outcome.Success -> outcome.data

                    // The repository has already retried. A palette is not worth holding the
                    // splash screen over, so the default is applied and the app starts.
                    is Outcome.Failure -> {
                        logger.w { "Theme unreadable (${outcome.error}); using the default" }
                        ThemePreference.DEFAULT
                    }
                }
            }
        }
    }

    private companion object {

        const val TAG = "MainViewModel"

        /** The outlet key the launch after a crash posts under. */
        const val CRASHED_MESSAGE = "main.crashed"

        /** The outlet key the session-expired notice is posted under: a second one replaces it. */
        const val SESSION_EXPIRED_MESSAGE = "main.session_expired"
        const val UPDATE_DOWNLOADED_MESSAGE = "main.update_downloaded"

        /**
         * The flow both stored facts add up to.
         *
         * The tour wins over the session: a stored session on a device that has not seen the tour
         * means the app was reinstalled over one, not that the tour was taken.
         */
        fun flowFor(signedIn: Boolean?, onboardingSeen: Boolean?): SessionState = when {
            signedIn == null || onboardingSeen == null -> SessionState.Unknown
            !onboardingSeen -> SessionState.Onboarding
            signedIn -> SessionState.SignedIn
            else -> SessionState.SignedOut
        }
    }
}
