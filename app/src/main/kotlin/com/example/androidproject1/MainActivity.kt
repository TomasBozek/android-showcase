package com.example.androidproject1

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.example.androidproject1.config.AppHold
import com.example.androidproject1.config.UpdateBanner
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.overlay.AppDialog
import com.example.androidproject1.core.ui.component.status.AppBanner
import com.example.androidproject1.core.ui.layout.LocalCoachTargets
import com.example.androidproject1.core.ui.layout.rememberCoachTargets
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.apphold.api.AppHoldDestination
import com.example.androidproject1.feature.apphold.api.AppHoldReason
import com.example.androidproject1.feature.auth.api.LoginDestination
import com.example.androidproject1.feature.home.api.HomeDestination
import com.example.androidproject1.feature.onboarding.api.OnboardingDestination
import com.example.androidproject1.feature.onboarding.presentation.component.WhatsNewSheet
import com.example.androidproject1.feature.onboarding.presentation.tour.TourOverlay
import com.example.androidproject1.feature.onboarding.presentation.tour.TourViewModel
import com.example.androidproject1.feature.settings.domain.ThemePreference
import com.example.androidproject1.message.MessageBanner
import com.example.androidproject1.message.MessageDialog
import com.example.androidproject1.message.ServerMessage
import com.example.androidproject1.service.navigation.AppNavigator
import com.example.androidproject1.service.navigation.LocalAppNavigator
import com.example.androidproject1.service.navigation.TabbedNavigator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The app's only Activity; everything else is a composable destination.
 *
 * An `AppCompatActivity` for one reason: below API 33 it is what applies the stored per-app
 * language in `attachBaseContext`, before the first frame. Nothing else of AppCompat is used — the
 * theme's parent is the AppCompat one because the base class insists, and the window is all it
 * styles.
 */
class MainActivity : AppCompatActivity() {

    // internal rather than private: `MainActivityDeepLinkTest` recreates this activity and asserts
    // that the launch intent was not read a second time, which is only visible on the ViewModel
    // that outlived the recreation.
    internal val viewModel by viewModel<MainViewModel>()

    override fun onResume() {
        super.onResume()
        viewModel.checkForUpdate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, so the system splash screen is installed before the first frame.
        // It stays up for exactly as long as reading the session takes — there is no minimum hold,
        // and nothing to tune if that read ever grows into real startup work.
        // Both reads, not just the session: the theme decides what the first frame is painted
        // in, so drawing before it lands is a light flash in front of someone who chose dark.
        installSplashScreen().setKeepOnScreenCondition {
            viewModel.sessionState.value == SessionState.Unknown ||
                viewModel.theme.value == null ||
                // The app lock has not read its switch: neither the lock nor the app, yet.
                viewModel.locked.value == null
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Only on a real cold start. `onCreate` runs again on every rotation and after process
        // death with the same launch intent still attached, and re-reading it there re-applied the
        // link — the synthesised path was rebuilt and whatever the user had navigated to since was
        // thrown away. A non-null `savedInstanceState` is exactly "this is a recreation", and the
        // back stack that comes back with it already holds the link's own keys.
        //
        // `coldStart = true`: the app was launched by this link, so there is no back stack
        // behind the product and one has to be synthesised or Up closes the app.
        if (savedInstanceState == null) {
            viewModel.onDeepLink(intent.deepLinkUri(), coldStart = true)
        }

        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()

            // No KoinContext wrapper: since Koin 4.2, startKoin() sets the Compose context up.
            AppTheme(darkTheme = theme.isDark()) {
                val session by viewModel.sessionState.collectAsStateWithLifecycle()

                // Starts empty and is filled once the session says which flow the user is in — and
                // must be composed on the very first frame either way: rememberNavBackStack is a
                // rememberSaveable, and one that first enters composition on a later frame gets
                // nothing back from the restored state. Composing it behind the session is how the
                // back stack was lost after process death.
                val backStack = rememberNavBackStack()

                // Which flow the user is in *is* the identity of the stack's first key, so a
                // session change is applied by asserting that rather than by remembering whether it
                // has already been applied. A stack restored into the matching flow is left alone.
                val rootKey = session.rootKey()
                LaunchedEffect(rootKey) {
                    if (rootKey != null && backStack.firstOrNull() != rootKey) {
                        backStack.switchTo(rootKey)
                    }
                }

                // Applied after the flow has put its root on the stack, and only in the
                // signed-in one: a link followed while signed out lands on Login, and the
                // pending keys wait rather than being dropped. The exception is a reset link,
                // which is followed by someone who cannot sign in — it lands on whatever flow
                // is up, once one is.
                val deepLink by viewModel.deepLink.collectAsStateWithLifecycle()
                // The activity's own, so the same instance the tour is drawn from below.
                val tourViewModel: TourViewModel = koinViewModel()
                LaunchedEffect(deepLink, session) {
                    val ready = session == SessionState.SignedIn ||
                        (session != SessionState.Unknown && viewModel.deepLinkLandsWithoutSession)
                    if (deepLink.isNotEmpty() && ready) {
                        backStack.applyDeepLink(deepLink)
                        viewModel.onDeepLinkApplied()
                        // A link is somewhere the user asked to go; a tour over it is in the way.
                        // Paused for this launch, not marked seen.
                        tourViewModel.pause()
                    }
                }

                val online by viewModel.online.collectAsStateWithLifecycle()
                // The banner sits above the display and inside the theme, so the splash is
                // untouched and every screen keeps its own scaffold. It spends the status-bar
                // inset itself and consumes it for what follows — otherwise the screen below
                // would pad for a bar the banner already covers. Outside every AppScaffold, so
                // `testTagsAsResourceId` is switched on here for a flow to find it. Composed
                // always and shown by `visible`, so the bar below slides rather than jumps.
                // Every element a tour can point at registers its bounds here, so the
                // registry is provided around the whole display rather than per screen — the
                // menu icon and the tabs belong to no screen at all.
                val coachTargets = rememberCoachTargets()
                CompositionLocalProvider(
                    LocalCoachTargets provides coachTargets,
                    // What `Screen()` opens a message's `UiCommand.Navigate` through.
                    LocalAppNavigator provides koinInject<AppNavigator>(),
                ) {
                    Column(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                        AppBanner(
                            text = stringResource(R.string.main_offline),
                            visible = !online,
                            modifier = Modifier
                                .statusBarsPadding()
                                .testTag("main_offlineBadge"),
                        )
                        // Nothing to display until the session has named a flow, or a saved
                        // stack has come back. The splash screen is what the user sees until
                        // then.
                        // Held by the startup config: the hold screen is all there is — no
                        // display under it, so neither back nor a link reaches past it.
                        val hold by viewModel.hold.collectAsStateWithLifecycle()
                        val storeUrl = STORE_URL + packageName
                        hold?.let {
                            AppHoldHost(
                                hold = AppHoldDestination(
                                    reason = when (it) {
                                        AppHold.UpdateRequired -> AppHoldReason.UpdateRequired
                                        AppHold.Maintenance -> AppHoldReason.Maintenance
                                    },
                                    storeUrl = storeUrl,
                                ),
                            )
                        }
                        if (hold == null && backStack.isNotEmpty()) {
                            // The tour draws over the display and under the banner: it points
                            // at what is on screen, and the banner is about the network, which
                            // is a different thing and stays readable.
                            Box {
                                AppNavHost(
                                    backStack = backStack,
                                    modifier = Modifier.consumeWindowInsets(
                                        if (online) WindowInsets(0, 0, 0, 0) else WindowInsets.statusBars,
                                    ),
                                    onReplayTour = tourViewModel::replay,
                                    homeHeader = {
                                        val message by viewModel.message.collectAsStateWithLifecycle()
                                        message?.takeIf { it.placement == ServerMessage.Placement.Banner }?.let {
                                            MessageBanner(it, viewModel::followMessage, viewModel::dismissMessage)
                                        }
                                        val updateOffer by viewModel.updateOffer.collectAsStateWithLifecycle()
                                        val uriHandler = LocalUriHandler.current
                                        if (updateOffer != null) {
                                            UpdateBanner(
                                                onUpdate = { uriHandler.openUri(storeUrl) },
                                                onDismiss = viewModel::dismissUpdate,
                                            )
                                        }
                                    },
                                )
                                // Held by the app lock: its screen over the display, which stays
                                // where it was underneath. A dialog draws in a window of its own, above
                                // everything, so none is shown until the app is let in.
                                val locked by viewModel.locked.collectAsStateWithLifecycle()
                                if (locked == true) {
                                    AppLockHost(modifier = Modifier.fillMaxSize())
                                } else {
                                    Tour(viewModel = tourViewModel, session = session)
                                    TermsUpdate()
                                    ServerMessageDialog()
                                    WhatsNew()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The re-ask when the terms changed under a signed-in account: over whatever screen is
     * up, and not dismissable by a tap outside — the answer is accept or sign out, and a dialog
     * that went away on its own would come back on the next start with nothing changed.
     */
    @Composable
    private fun TermsUpdate() {
        val outdated by viewModel.termsOutdated.collectAsStateWithLifecycle()
        if (!outdated) return
        AppDialog(
            title = stringResource(R.string.main_terms_title),
            message = stringResource(R.string.main_terms_message),
            onDismiss = {},
            modifier = Modifier.testTag("main_termsDialog"),
            actions = {
                AppButton(
                    label = stringResource(R.string.main_terms_decline),
                    onClick = viewModel::declineTerms,
                    kind = ButtonKind.Ghost,
                    modifier = Modifier.testTag("main_termsDeclineButton"),
                )
                AppButton(
                    label = stringResource(R.string.main_terms_accept),
                    onClick = viewModel::acceptTerms,
                    modifier = Modifier.testTag("main_termsAcceptButton"),
                )
            },
        )
    }

    /** What changed in this update, once per update, over whichever flow is up. */
    @Composable
    private fun WhatsNew() {
        val show by viewModel.whatsNew.collectAsStateWithLifecycle()
        if (show) WhatsNewSheet(onDone = viewModel::whatsNewSeen)
    }

    /** A server message placed as a dialog; a banner one is Home's, through its header. */
    @Composable
    private fun ServerMessageDialog() {
        val message by viewModel.message.collectAsStateWithLifecycle()
        message?.takeIf { it.placement == ServerMessage.Placement.Dialog }?.let {
            MessageDialog(it, viewModel::followMessage, viewModel::dismissMessage)
        }
    }

    /**
     * The tour, offered once after the first sign-in and replayable from Settings or the menu.
     *
     * The activity owns it because it is drawn over whatever screen is up and points at elements
     * — the menu icon, the tabs — that belong to no screen. It is offered whenever the user is
     * signed in; the view model is what decides whether it actually runs, so the activity keeps
     * no "have I already asked" state of its own.
     */
    @Composable
    private fun Tour(viewModel: TourViewModel, session: SessionState) {
        val tour by viewModel.state.collectAsStateWithLifecycle()
        val steps = tourSteps(rememberTourStrings())
        LaunchedEffect(session, steps) {
            if (session == SessionState.SignedIn) viewModel.start(steps)
        }
        TourOverlay(state = tour, onEvent = viewModel::onEvent)
    }

    /**
     * A link that arrived while the app was already running.
     *
     * `coldStart = false`: there is a back stack, and the user's place in it is theirs. The
     * product is pushed onto whatever tab they were on, so Up returns them to it.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onDeepLink(intent.deepLinkUri(), coldStart = false)
    }

    /**
     * The link this intent carries, if it is one of ours.
     *
     * The scheme is checked here, against the same `applicationId` the manifest's filter is
     * built from, so there is one place that says what the app's scheme is and nothing to keep
     * in step. `DeepLinks` then parses a plain string and stays an ordinary JVM function.
     */
    private fun Intent.deepLinkUri(): String? =
        data?.takeIf { it.scheme == BuildConfig.APPLICATION_ID }?.toString()
}

/**
 * Whether to draw dark, for a preference that may not have been read yet.
 *
 * `null` and [ThemePreference.System] answer the same way — follow the device — so an
 * unfinished read looks like the default rather than like a choice nobody made.
 */
@Composable
private fun ThemePreference?.isDark(): Boolean = when (this) {
    ThemePreference.Light -> false
    ThemePreference.Dark -> true
    ThemePreference.System, null -> isSystemInDarkTheme()
}

/** The first key of the flow this state belongs in, or `null` while it is not known yet. */
private fun SessionState.rootKey(): NavKey? = when (this) {
    SessionState.Unknown -> null
    SessionState.Onboarding -> OnboardingDestination
    SessionState.SignedIn -> HomeDestination
    SessionState.SignedOut -> LoginDestination
}

/**
 * Replaces the whole back stack with [start].
 *
 * Navigation 3's back stack is a plain list, so switching flows is a list operation rather than a
 * `popUpTo(0) { inclusive = true }` incantation.
 */

private fun NavBackStack<NavKey>.switchTo(start: NavKey) {
    clear()
    add(start)
}

/**
 * Puts a link's keys on the stack.
 *
 * A cold start hands over the whole path and replaces what is there — which is the flow's root
 * and nothing else. A warm one hands over the target alone: a tab root is selected the way the
 * bar selects it, so the tab keeps the screens the user left on it; anything else is
 * pushed, so the user's place is kept and Up returns them to it. Either way the last key is not
 * added twice when it is already on top, which is what a second tap on the same link would do.
 */
private fun NavBackStack<NavKey>.applyDeepLink(keys: List<NavKey>) {
    if (keys.isEmpty() || lastOrNull() == keys.last()) return
    val tab = keys.singleOrNull()?.let { TopLevelDestination.of(it) }
    if (tab != null) {
        TabbedNavigator(this, TopLevelDestination.roots).selectTab(tab.key)
        return
    }
    if (keys.size > 1) clear()
    addAll(keys)
}

/** This app's store page, the package name appended: where an update, forced or offered, leads. */
private const val STORE_URL = "https://play.google.com/store/apps/details?id="
