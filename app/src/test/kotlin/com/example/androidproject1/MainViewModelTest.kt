package com.example.androidproject1

import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.config.AppConfig
import com.example.androidproject1.config.AppConfigRepository
import com.example.androidproject1.config.AppHold
import com.example.androidproject1.feature.auth.domain.Session
import com.example.androidproject1.feature.auth.domain.TermsPolicy
import com.example.androidproject1.feature.auth.domain.test.FakeAppLock
import com.example.androidproject1.feature.auth.domain.test.FakeAuthService
import com.example.androidproject1.feature.catalog.api.CategoriesDestination
import com.example.androidproject1.feature.catalog.api.ProductDetailDestination
import com.example.androidproject1.feature.home.api.HomeDestination
import com.example.androidproject1.feature.onboarding.domain.test.FakeOnboardingRepository
import com.example.androidproject1.feature.settings.domain.ThemePreference
import com.example.androidproject1.feature.settings.domain.test.FakeThemeRepository
import com.example.androidproject1.feature.trips.api.TripsDestination
import com.example.androidproject1.feature.weather.api.ForecastDestination
import com.example.androidproject1.feature.weather.api.WeatherDestination
import com.example.androidproject1.message.MessageRepository
import com.example.androidproject1.message.ServerMessage
import com.example.androidproject1.service.core.domain.error.UnexpectedError
import com.example.androidproject1.service.core.ui.event.MessageAction
import com.example.androidproject1.service.core.ui.event.MessageDuration
import com.example.androidproject1.service.core.ui.test.FakeAppMessages
import com.example.androidproject1.service.core.ui.test.MainDispatcherRule
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.device.test.FakeDeviceInfo
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.navigation.AppNavigatorChannel
import com.example.androidproject1.service.network.ConnectivityMonitor
import com.example.androidproject1.service.play.UpdateKind
import com.example.androidproject1.service.play.UpdateOffer
import com.example.androidproject1.service.play.test.FakeAppUpdates
import com.example.androidproject1.service.session.DefaultSessionExpiry
import com.example.androidproject1.service.telemetry.CrashKind
import com.example.androidproject1.service.telemetry.CrashRecord
import com.example.androidproject1.service.telemetry.test.FakeCrashLog
import com.example.androidproject1.service.telemetry.test.FakeTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import com.example.androidproject1.service.core.ui.R as CoreR

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val logger = FakeLogger()
    private val authService = FakeAuthService()
    private val appLock = FakeAppLock()
    private val onboardingRepository = FakeOnboardingRepository()
    private val themeRepository = FakeThemeRepository()
    private val errorTracker = FakeTelemetry()

    private val sessionExpiry = DefaultSessionExpiry()

    private val messages = object : MessageRepository {
        var served = listOf(message("autumn", ServerMessage.Placement.Banner))
        val dismissedIds = mutableSetOf<String>()
        var fetches = 0

        override suspend fun messages(): List<ServerMessage> {
            fetches++
            return served
        }

        override suspend fun dismissed(): Set<String> = dismissedIds

        override suspend fun dismiss(id: String) {
            dismissedIds += id
        }

        override suspend fun resetDismissed() = dismissedIds.clear()
    }

    private fun message(
        id: String,
        placement: ServerMessage.Placement,
        from: String = "2026-01-01T00:00:00Z",
        until: String = "2027-01-01T00:00:00Z",
    ) = ServerMessage(
        id = id,
        placement = placement,
        title = "Title",
        body = "Body",
        action = "app://cart",
        from = from,
        until = until,
    )

    private fun signedIn() {
        onboardingRepository.seen.value = true
        authService.session.value = Session(id = "session-1", email = "ada@example.com", termsVersion = 2)
    }

    private val crashLog = FakeCrashLog()

    private fun viewModel() = MainViewModel(
        logger = logger,
        authService = authService,
        onboardingRepository = onboardingRepository,
        themeRepository = themeRepository,
        errorTracker = errorTracker,
        connectivity = ConnectivityMonitor.AlwaysOnline,
        sessionExpiry = sessionExpiry,
        termsPolicy = TermsPolicy(
            version = 2,
            termsUrl = "https://example.com/terms",
            privacyUrl = "https://example.com/privacy",
        ),
        messageRepository = messages,
        deviceInfo = FakeDeviceInfo(versionName = "1.6.0", versionCode = CURRENT_VERSION_CODE),
        clock = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC),
        appConfigRepository = appConfig,
        appMessages = appMessages,
        appNavigator = appNavigator,
        appUpdates = appUpdates,
        appLock = appLock,
        crashLog = crashLog,
    )

    private val appUpdates = FakeAppUpdates()

    private val appMessages = FakeAppMessages()
    private val appNavigator = AppNavigatorChannel()

    /**
     * The config seam: a refresh applies what the server says, or — offline — the
     * last answer this install kept, or nothing at all.
     */
    private val appConfig = object : AppConfigRepository {
        /** What the server answers; `null` is offline. */
        var served: AppConfig? = null

        /** What an earlier run kept. */
        var cached: AppConfig? = null

        override val config = MutableStateFlow<AppConfig?>(null)

        var dismissed = 0

        override suspend fun dismissedUpdate(): Int = dismissed

        override suspend fun dismissUpdate(versionCode: Int) {
            dismissed = versionCode
        }

        override suspend fun refresh() {
            config.value = served?.also { cached = it } ?: cached
        }
    }

    /**
     * The tour outranks the session, so every case about the session starts past it. A test
     * that forgets this asserts `Onboarding` and reads as a session bug.
     */
    @Before
    fun onboardingIsDone() {
        onboardingRepository.seen.value = true
    }

    @Test
    fun `the launch after a crash offers, once, to send the report`() = runTest {
        crashLog.records += CrashRecord(Instant.parse("2026-09-23T11:00:00Z"), CrashKind.Crash, "boom", "trace")

        viewModel()
        viewModel()

        val message = appMessages.posted.single { it.key == "main.crashed" }
        assertEquals(R.string.main_crashed.toUiText(), message.text)
        assertEquals(CrashReportHandler.KEY, (message.action as MessageAction.Handler).handlerKey)
    }

    @Test
    fun `a launch with no crash behind it says nothing about one`() = runTest {
        viewModel()

        assertTrue(appMessages.posted.none { it.key == "main.crashed" })
    }

    @Test
    fun `a build below the server's minimum is held for an update, signed out or not`() = runTest {
        appConfig.served =
            AppConfig(minVersionCode = CURRENT_VERSION_CODE + 1, latestVersionCode = CURRENT_VERSION_CODE + 1)

        val viewModel = viewModel()

        assertEquals(AppHold.UpdateRequired, viewModel.hold.value)
    }

    @Test
    fun `maintenance holds the app, and an update outranks it`() = runTest {
        appConfig.served = AppConfig(maintenance = true)
        assertEquals(AppHold.Maintenance, viewModel().hold.value)

        appConfig.served = AppConfig(minVersionCode = CURRENT_VERSION_CODE + 1, maintenance = true)
        assertEquals(AppHold.UpdateRequired, viewModel().hold.value)
    }

    @Test
    fun `offline, a hold the last answer set still holds`() = runTest {
        appConfig.cached = AppConfig(maintenance = true)

        assertEquals(AppHold.Maintenance, viewModel().hold.value)
    }

    @Test
    fun `offline with nothing cached, the app runs unheld`() = runTest {
        assertNull(viewModel().hold.value)
    }

    @Test
    fun `a newer build is offered, and dismissing it keeps that version from being offered again`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 1)
        val viewModel = viewModel()
        assertEquals(CURRENT_VERSION_CODE + 1, viewModel.updateOffer.value)

        viewModel.dismissUpdate()

        assertNull(viewModel.updateOffer.value)
        assertEquals(CURRENT_VERSION_CODE + 1, appConfig.dismissed)
        assertNull("a later start does not offer it either", viewModel().updateOffer.value)
    }

    @Test
    fun `a newer build than the one dismissed is offered again`() = runTest {
        appConfig.dismissed = CURRENT_VERSION_CODE + 1
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 2)

        assertEquals(CURRENT_VERSION_CODE + 2, viewModel().updateOffer.value)
    }

    @Test
    fun `this build being the latest offers nothing`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE)

        assertNull(viewModel().updateOffer.value)
    }

    @Test
    fun `a build at the minimum, with a newer one out, is not held`() = runTest {
        appConfig.served =
            AppConfig(minVersionCode = CURRENT_VERSION_CODE, latestVersionCode = CURRENT_VERSION_CODE + 5)

        assertNull(viewModel().hold.value)
    }

    @Test
    fun `a signed-in app the lock holds is locked, and unlocking lets it in`() = runTest {
        authService.session.value = Session(id = "session-1", email = "ada@example.com")
        appLock.held.value = true
        val viewModel = viewModel()
        assertEquals(true, viewModel.locked.value)

        appLock.unlocked()

        assertEquals(false, viewModel.locked.value)
    }

    @Test
    fun `a signed-out app is never locked, whatever the lock says`() = runTest {
        appLock.held.value = true

        assertEquals(false, viewModel().locked.value)
    }

    @Test
    fun `until the lock has read its switch, a signed-in app is neither locked nor open`() = runTest {
        authService.session.value = Session(id = "session-1", email = "ada@example.com")
        appLock.held.value = null

        assertNull(viewModel().locked.value)
    }

    @Test
    fun `a stored session reads as signed in`() = runTest {
        authService.session.value = Session(id = "session-1", email = "ada@example.com")

        assertEquals(SessionState.SignedIn, viewModel().sessionState.value)
    }

    @Test
    fun `no stored session reads as signed out`() = runTest {
        assertEquals(SessionState.SignedOut, viewModel().sessionState.value)
    }

    @Test
    fun `signing out later switches the session back`() = runTest {
        authService.session.value = Session(id = "session-1", email = "ada@example.com")
        val viewModel = viewModel()

        authService.logout()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
    }

    @Test
    fun `signing in later switches the session forward`() = runTest {
        // The other direction of `signing out later switches the session back`: nothing but the
        // session changing moves the app between the auth flow and the tabs, so both ways have to
        // be asserted or half the switch is untested.
        val viewModel = viewModel()
        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)

        authService.login("ada@example.com")

        assertEquals(SessionState.SignedIn, viewModel.sessionState.value)
        assertEquals(listOf(null, FakeAuthService.SESSION_ID), errorTracker.users)
    }

    @Test
    fun `a session that becomes unreadable mid-flight falls back to signed out`() = runTest {
        // `observeSession()` has already retried by the time a failure reaches here, and there is
        // no screen to put a dialog over — so the flow drops to the one state the user can act
        // from, and says so in the log rather than in their face.
        authService.session.value = Session(id = "session-1", email = "ada@example.com")
        val viewModel = viewModel()
        assertEquals(SessionState.SignedIn, viewModel.sessionState.value)

        authService.sessionError = UnexpectedError(message = "disk gone")
        authService.session.value = null
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
        assertTrue(logger.warnings.isNotEmpty())
    }

    @Test
    fun `an unreadable session is signed out, with a warning rather than a dialog`() = runTest {
        authService.sessionError = UnexpectedError(message = "disk gone")

        assertEquals(SessionState.SignedOut, viewModel().sessionState.value)
        assertTrue(logger.warnings.isNotEmpty())
    }

    @Test
    fun `an unfinished tour is the onboarding flow`() = runTest {
        onboardingRepository.seen.value = false

        assertEquals(SessionState.Onboarding, viewModel().sessionState.value)
    }

    @Test
    fun `an unfinished tour outranks a stored session`() = runTest {
        // A stored session on a device that has not seen the tour means a reinstall over one,
        // not that the tour was taken.
        onboardingRepository.seen.value = false
        authService.session.value = Session(id = "session-1", email = "ada@example.com")

        assertEquals(SessionState.Onboarding, viewModel().sessionState.value)
    }

    @Test
    fun `finishing the tour moves the app on without the screen navigating`() = runTest {
        onboardingRepository.seen.value = false
        val viewModel = viewModel()

        onboardingRepository.markSeen()
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
    }

    @Test
    fun `an unreadable onboarding flag skips the tour rather than repeating it`() = runTest {
        onboardingRepository.failWith = UnexpectedError(message = "disk gone")

        assertEquals(SessionState.SignedOut, viewModel().sessionState.value)
        assertTrue(logger.warnings.isNotEmpty())
    }

    @Test
    fun `the session is unknown until it has been read`() = runTest {
        // Overrides the rule's UnconfinedTestDispatcher for this one case: under that dispatcher
        // the collections started in `init` have already produced a value by the time the
        // constructor returns, which is exactly what hides the state under test. Unknown holds
        // until *both* the session and the first-run flag have answered.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel()

        assertEquals(SessionState.Unknown, viewModel.sessionState.value)

        advanceUntilIdle()
        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
    }

    @Test
    fun `the stored theme is what the app draws in`() = runTest {
        themeRepository.theme.value = ThemePreference.Dark

        assertEquals(ThemePreference.Dark, viewModel().theme.value)
    }

    @Test
    fun `changing the theme reaches the root`() = runTest {
        val viewModel = viewModel()

        themeRepository.setTheme(ThemePreference.Light)
        advanceUntilIdle()

        assertEquals(ThemePreference.Light, viewModel.theme.value)
    }

    @Test
    fun `the theme is null until it has been read, so the splash holds`() = runTest {
        // Same dispatcher swap as the session case above, and for the same reason.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel()

        assertNull(viewModel.theme.value)

        advanceUntilIdle()
        assertEquals(ThemePreference.System, viewModel.theme.value)
    }

    @Test
    fun `an unreadable theme falls back to the default rather than holding the splash`() = runTest {
        themeRepository.failWith = UnexpectedError(message = "disk gone")

        assertEquals(ThemePreference.DEFAULT, viewModel().theme.value)
        assertTrue(logger.warnings.isNotEmpty())
    }

    @Test
    fun `a cold product link opens on Home, Categories, the product`() = runTest {
        val viewModel = viewModel()

        viewModel.onDeepLink("app://product/coffee", coldStart = true)
        advanceUntilIdle()

        // The catalog table's own path under Home: the shell looks nothing up, so Up walks
        // back through Categories rather than closing the app.
        assertEquals(
            listOf(HomeDestination, CategoriesDestination, ProductDetailDestination(productId = "coffee")),
            viewModel.deepLink.value,
        )
    }

    @Test
    fun `a warm-start deep link pushes only the product`() = runTest {
        val viewModel = viewModel()

        viewModel.onDeepLink("app://product/coffee", coldStart = false)
        advanceUntilIdle()

        // The user's place in the stack is theirs; the product goes on top of it.
        assertEquals(listOf(ProductDetailDestination(productId = "coffee")), viewModel.deepLink.value)
    }

    @Test
    fun `a tab link is Home under the tab cold, and the tab alone warm`() = runTest {
        val viewModel = viewModel()

        viewModel.onDeepLink("app://trips", coldStart = true)
        advanceUntilIdle()
        assertEquals(listOf(HomeDestination, TripsDestination), viewModel.deepLink.value)

        viewModel.onDeepLink("app://trips", coldStart = false)
        advanceUntilIdle()
        // A single tab key: the activity selects the tab rather than pushing it.
        assertEquals(listOf<NavKey>(TripsDestination), viewModel.deepLink.value)
    }

    @Test
    fun `a place link is Home, Weather and the forecast cold, and the forecast alone warm`() = runTest {
        val viewModel = viewModel()

        viewModel.onDeepLink("app://weather/3067696", coldStart = true)
        advanceUntilIdle()
        assertEquals(
            listOf(HomeDestination, WeatherDestination, ForecastDestination(placeId = 3067696)),
            viewModel.deepLink.value,
        )

        viewModel.onDeepLink("app://weather/3067696", coldStart = false)
        advanceUntilIdle()
        assertEquals(listOf<NavKey>(ForecastDestination(placeId = 3067696)), viewModel.deepLink.value)
    }

    @Test
    fun `a link that is not one of ours leaves the stack alone`() = runTest {
        val viewModel = viewModel()

        viewModel.onDeepLink("app://invoice/1", coldStart = true)
        advanceUntilIdle()

        assertTrue(viewModel.deepLink.value.isEmpty())
    }

    @Test
    fun `an applied deep link is not applied twice`() = runTest {
        val viewModel = viewModel()
        viewModel.onDeepLink("app://product/coffee", coldStart = false)
        advanceUntilIdle()

        viewModel.onDeepLinkApplied()

        assertTrue(viewModel.deepLink.value.isEmpty())
    }

    @Test
    fun `a signed-in session is reported to the crash tracker by id`() = runTest {
        authService.session.value = Session(id = "session-1", email = "ada@example.com")

        viewModel()

        // The id, never the address: a crash report is not the place for one.
        assertEquals(listOf("session-1"), errorTracker.users)
    }

    @Test
    fun `signing out clears the reported user`() = runTest {
        authService.session.value = Session(id = "session-1", email = "ada@example.com")
        val viewModel = viewModel()

        authService.session.value = null
        advanceUntilIdle()

        // Null on the way out, or the next person's reports are attributed to the last one.
        assertEquals(listOf("session-1", null), errorTracker.users)
        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
    }

    @Test
    fun `an unreadable session clears the reported user too`() = runTest {
        authService.failWith = UnexpectedError()

        viewModel()

        assertEquals(listOf(null), errorTracker.users)
    }

    @Test
    fun `a 401 signs the session out once, and says why through the outlet`() = runTest {
        onboardingRepository.seen.value = true
        authService.session.value = Session(id = "session-1", email = "ada@example.com")
        val viewModel = viewModel()
        assertEquals(SessionState.SignedIn, viewModel.sessionState.value)

        sessionExpiry.signal()
        sessionExpiry.signal()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
        assertEquals(1, authService.logoutCount)
        // Said once, by the outlet, to whichever screen is up — Login, now — rather than a mark
        // the sign-in screen reads on opening.
        assertEquals(
            listOf(CoreR.string.core_error_session_expired.toUiText()),
            appMessages.posted.map { it.text },
        )
    }

    @Test
    fun `a 401 while signed out is ignored`() = runTest {
        onboardingRepository.seen.value = true
        val viewModel = viewModel()
        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)

        sessionExpiry.signal()

        assertEquals(0, authService.logoutCount)
        assertTrue(appMessages.posted.isEmpty())
    }

    @Test
    fun `a class that is not a screen moves the user through the app navigator`() = runTest {
        signedIn()
        val viewModel = viewModel()

        // Anything with the navigator injected — a worker, a repository — asks the same way.
        appNavigator.navigate(listOf(ForecastDestination(placeId = 3067696)))

        assertEquals(listOf<NavKey>(ForecastDestination(placeId = 3067696)), viewModel.deepLink.value)
    }

    @Test
    fun `a session that accepted older terms is asked again, and accepting stores the new version`() = runTest {
        onboardingRepository.seen.value = true
        authService.session.value = Session(id = "session-1", email = "ada@example.com", termsVersion = 1)
        val viewModel = viewModel()

        assertTrue(viewModel.termsOutdated.value)

        viewModel.acceptTerms()

        assertEquals(2, authService.session.value?.termsVersion)
        assertFalse(viewModel.termsOutdated.value)
        assertEquals(SessionState.SignedIn, viewModel.sessionState.value)
    }

    @Test
    fun `declining the new terms signs out`() = runTest {
        onboardingRepository.seen.value = true
        authService.session.value = Session(id = "session-1", email = "ada@example.com", termsVersion = 1)
        val viewModel = viewModel()

        viewModel.declineTerms()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
        assertFalse(viewModel.termsOutdated.value)
    }

    @Test
    fun `a session on the current terms is not asked`() = runTest {
        onboardingRepository.seen.value = true
        authService.session.value = Session(id = "session-1", email = "ada@example.com", termsVersion = 2)

        assertFalse(viewModel().termsOutdated.value)
    }

    // -- a message from the server --------------------------------------------------

    @Test
    fun `a live banner is shown once signed in, and fetched once per start`() = runTest {
        signedIn()

        val viewModel = viewModel()

        assertEquals("autumn", viewModel.message.value?.id)
        assertEquals(1, messages.fetches)
    }

    @Test
    fun `a dialog message is shown the same way, as a dialog`() = runTest {
        messages.served = listOf(message("outage", ServerMessage.Placement.Dialog))
        signedIn()

        assertEquals(ServerMessage.Placement.Dialog, viewModel().message.value?.placement)
    }

    @Test
    fun `a dismissed message is never shown again`() = runTest {
        signedIn()
        val viewModel = viewModel()

        viewModel.dismissMessage()

        assertNull(viewModel.message.value)
        assertEquals(setOf("autumn"), messages.dismissedIds)
        assertNull(viewModel().message.value)
    }

    @Test
    fun `a message outside its window is not shown`() = runTest {
        messages.served = listOf(
            message(
                "spring",
                ServerMessage.Placement.Banner,
                from = "2027-03-01T00:00:00Z",
                until = "2027-06-01T00:00:00Z",
            ),
        )
        signedIn()

        assertNull(viewModel().message.value)
    }

    @Test
    fun `nothing is fetched while signed out`() = runTest {
        onboardingRepository.seen.value = true

        val viewModel = viewModel()

        assertNull(viewModel.message.value)
        assertEquals(0, messages.fetches)
    }

    @Test
    fun `the action follows its link and puts the message away`() = runTest {
        signedIn()
        val viewModel = viewModel()

        viewModel.followMessage()

        assertEquals(listOf<NavKey>(TopLevelDestination.Cart.key), viewModel.deepLink.value)
        assertNull(viewModel.message.value)
        assertEquals(setOf("autumn"), messages.dismissedIds)
    }

    // -- what's new ------------------------------------------------------------------

    @Test
    fun `a first run stores the current version and shows nothing`() = runTest {
        onboardingRepository.lastSeenVersionCode = 0
        signedIn()

        val viewModel = viewModel()

        assertFalse(viewModel.whatsNew.value)
        assertEquals(CURRENT_VERSION_CODE, onboardingRepository.lastSeenVersionCode)
    }

    private fun flexibleOffer() = UpdateOffer(
        versionCode = CURRENT_VERSION_CODE + 1,
        staleDays = 1,
        priority = 0,
        immediateAllowed = false,
        flexibleAllowed = true,
    )

    @Test
    fun `a newer build the server names starts Play's flexible update in the background`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 1)
        appUpdates.offer = flexibleOffer()

        viewModel()

        assertEquals(listOf(UpdateKind.Flexible), appUpdates.started)
    }

    @Test
    fun `an offer that appears later is taken on the next resume, and asked for once`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 1)
        val viewModel = viewModel()
        assertTrue("Play had nothing at start", appUpdates.started.isEmpty())

        appUpdates.offer = flexibleOffer()
        viewModel.checkForUpdate()
        viewModel.checkForUpdate()

        assertEquals(listOf(UpdateKind.Flexible), appUpdates.started)
    }

    @Test
    fun `no newer build, no Play offer, or a held app starts nothing`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE)
        appUpdates.offer = flexibleOffer()
        viewModel()
        assertTrue(appUpdates.started.isEmpty())

        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 1)
        appUpdates.offer = null
        viewModel()
        assertTrue(appUpdates.started.isEmpty())

        appUpdates.offer = flexibleOffer()
        appConfig.served =
            AppConfig(minVersionCode = CURRENT_VERSION_CODE + 1, latestVersionCode = CURRENT_VERSION_CODE + 1)
        viewModel()
        assertTrue("a held app is the hold screen's to update", appUpdates.started.isEmpty())
    }

    @Test
    fun `a downloaded flexible update posts a snackbar whose action completes it`() = runTest {
        viewModel()

        appUpdates.finishDownload()

        val message = appMessages.posted.single { it.key == "main.update_downloaded" }
        val action = message.action as MessageAction.Handler
        assertEquals(UpdateRestartHandler.KEY, action.handlerKey)
        assertEquals(MessageDuration.Indefinite, message.duration)
        assertEquals(0, appUpdates.completed)

        UpdateRestartHandler(appUpdates).handle(action.argument)

        assertEquals(1, appUpdates.completed)
    }

    @Test
    fun `an update shows what's new once, and done stores the version`() = runTest {
        onboardingRepository.lastSeenVersionCode = CURRENT_VERSION_CODE - 1
        signedIn()
        val viewModel = viewModel()

        assertTrue(viewModel.whatsNew.value)

        viewModel.whatsNewSeen()

        assertFalse(viewModel.whatsNew.value)
        assertEquals(CURRENT_VERSION_CODE, onboardingRepository.lastSeenVersionCode)
        assertFalse(viewModel().whatsNew.value)
    }

    @Test
    fun `the same version shows nothing`() = runTest {
        onboardingRepository.lastSeenVersionCode = CURRENT_VERSION_CODE
        signedIn()

        assertFalse(viewModel().whatsNew.value)
    }

    private companion object {

        const val CURRENT_VERSION_CODE = 412
    }
}
