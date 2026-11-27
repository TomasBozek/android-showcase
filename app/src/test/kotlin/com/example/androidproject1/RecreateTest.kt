package com.example.androidproject1

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ActivityScenario
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.domain.TermsPolicy
import com.example.androidproject1.feature.onboarding.domain.OnboardingRepository
import com.example.androidproject1.service.core.data.crypto.AesGcmAead
import com.example.androidproject1.service.core.domain.crypto.Aead
import com.example.androidproject1.service.navigation.AppNavigator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

/**
 * Every place the shell can show comes back from a recreation — a rotation, a fold, a large
 * screen ignoring the orientation lock at targetSdk 37 — as the same screen over the same stack.
 *
 * The real `App`, its fixture graph and the real [AppNavHost]: the trap this holds is
 * `MainActivity`'s, `rememberNavBackStack` composed on the first frame, and a test over a stand-in
 * activity would not see it. Each place is a case: the five tab roots, and every [ShellJump] the
 * features bind — read from Koin with `getAll()`, so a feature's new jump is covered without an edit
 * here. "The same stack" is walked rather than read: the place is on top after the recreation, and
 * back from it lands on Home, which is what was under it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class)
class RecreateTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    /** Every case runs, and every one that fails is named, rather than stopping at the first. */
    @get:Rule
    val errors = ErrorCollector()

    private val koin get() = GlobalContext.get()

    // Signed in, past onboarding, on the current terms and with the tour seen: Home is the root,
    // with nothing drawn over it that would take a back press.
    @Before
    fun signIn() = runBlocking {
        // Robolectric has no AndroidKeyStore, so the session's cipher is the same AES-GCM over a
        // key made here (CODEBASE § Known constraints) — before anything has asked for it.
        val key = KeyGenerator.getInstance("AES").apply { init(KEY_BITS) }.generateKey()
        loadKoinModules(module { single<Aead> { AesGcmAead { key } } })
        koin.get<OnboardingRepository>().apply {
            markSeen()
            markTourSeen()
        }
        val auth = koin.get<AuthService>()
        auth.login("ada@example.com")
        auth.acceptTerms(koin.get<TermsPolicy>().version)
        Unit
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun `every tab root comes back from a recreation`() {
        TopLevelDestination.entries.forEach { tab ->
            check(tab.name, tab.key, screenIdOf(tab), pushed = tab != TopLevelDestination.Home)
        }
    }

    @Test
    fun `every jump comes back from a recreation`() {
        val jumps = koin.getAll<ShellJump>().sortedBy { it.id }
        assertTrue("the devDebug graph binds the features' jumps", jumps.isNotEmpty())

        jumps.forEach { jump -> check(jump.id, jump.route, jump.screenId, pushed = true) }
    }

    /** Opens [route] over Home, recreates, and asserts [screenId] is still on top of Home. */
    private fun check(case: String, route: NavKey, screenId: String, pushed: Boolean) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            errors.checkSucceeds {
                awaitScreen(case, "HomeScreen")
                koin.get<AppNavigator>().navigate(listOf(route))
                awaitScreen(case, screenId)

                scenario.recreate()

                awaitScreen("$case after the recreation", screenId)
                if (pushed) {
                    assertTrue("$case: Home is not drawn over it", !shows("HomeScreen"))
                    scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                    awaitScreen("$case: back after the recreation", "HomeScreen")
                }
            }
        }
    }

    private fun shows(screenId: String) =
        compose.onAllNodesWithTag(screenId, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun awaitScreen(case: String, screenId: String) {
        val deadline = System.currentTimeMillis() + WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (shows(screenId)) {
                compose.waitForIdle()
                return
            }
            Thread.sleep(STEP_MS)
        }
        throw AssertionError("$case: $screenId was not shown within $WAIT_MS ms; shown: ${shownScreens()}")
    }

    /** The screens on display, for a failure to say where it was instead. */
    private fun shownScreens(): List<String> = compose.onAllNodes(
        hasTestTagEndingWith("Screen"),
        useUnmergedTree = true,
    )
        .fetchSemanticsNodes()
        .map { it.config[SemanticsProperties.TestTag] }

    private fun hasTestTagEndingWith(suffix: String) = SemanticsMatcher("testTag ends with $suffix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.endsWith(suffix) == true
    }

    /** Exhaustive, so a new tab does not compile until it says which screen is its root. */
    private fun screenIdOf(tab: TopLevelDestination) = when (tab) {
        TopLevelDestination.Home -> "HomeScreen"
        TopLevelDestination.Catalog -> "CategoriesScreen"
        TopLevelDestination.Cart -> "CartScreen"
        TopLevelDestination.Trips -> "TripsScreen"
        TopLevelDestination.Settings -> "SettingsScreen"
    }

    private companion object {

        const val STEP_MS = 20L
        const val WAIT_MS = 5_000L
        const val KEY_BITS = 256
    }
}
