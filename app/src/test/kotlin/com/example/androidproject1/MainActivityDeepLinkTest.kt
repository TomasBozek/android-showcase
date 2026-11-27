package com.example.androidproject1

import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A cold-start deep link is applied once, not once per activity creation.
 *
 * `onCreate` runs again on every rotation with the same launch intent still attached. Re-reading
 * it there rebuilt the synthesised Home → Categories → category → product path and threw away
 * whatever the user had navigated to since — silently, and only on a real device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class)
class MainActivityDeepLinkTest {

    // `App` starts the real graph, and it lives in the same JVM as every other test in this
    // module — one that calls startKoin itself fails on a graph this test left running.
    @After
    fun tearDown() = stopKoin()

    // Launches nothing itself; it reads the Compose tree of the activity a test launches.
    @get:Rule
    val compose = createEmptyComposeRule()

    private fun launchIntent(path: String = "product/coffee") = Intent(Intent.ACTION_VIEW).apply {
        setClassName(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<App>(),
            MainActivity::class.java.name,
        )
        data = Uri.parse("${BuildConfig.APPLICATION_ID}://$path")
    }

    /**
     * Runs the main looper until [done] holds, for as long as [WAIT_MS]; the session and the path
     * are read off the main thread. It fails rather than returning an unsettled activity: at one
     * second it gave up silently under the whole-repo run's load, and the assertion after it
     * reported a deep link that had simply not been looked up yet.
     */
    private fun ActivityScenario<MainActivity>.awaitUntil(done: (MainActivity) -> Boolean): MainActivity {
        lateinit var activity: MainActivity
        repeat((WAIT_MS / SETTLE_STEP_MS).toInt()) {
            shadowOf(Looper.getMainLooper()).idle()
            onActivity { activity = it }
            if (done(activity)) return activity
            Thread.sleep(SETTLE_STEP_MS)
        }
        throw AssertionError("the activity did not settle within $WAIT_MS ms")
    }

    @Test
    fun `the launch intent is read on the first creation only`() {
        ActivityScenario.launch<MainActivity>(launchIntent()).use { scenario ->
            // The path is looked up off the main thread, so the keys are waited for rather than
            // assumed; then handed over, as the back stack would.
            scenario.awaitUntil { it.viewModel.deepLink.value.isNotEmpty() }
            scenario.onActivity { it.viewModel.onDeepLinkApplied() }

            scenario.recreate()

            // The ViewModel outlives the recreation, so a link re-read from the intent would show
            // up here as keys pending all over again.
            scenario.onActivity { activity ->
                assertEquals(emptyList<Any>(), activity.viewModel.deepLink.value)
            }
        }
    }

    @Test
    fun `a reset link lands without a session, where every other link waits`() {
        ActivityScenario.launch<MainActivity>(launchIntent("reset/fixture")).use { scenario ->
            compose.waitUntil(timeoutMillis = WAIT_MS) {
                compose.onAllNodesWithTag("ResetPasswordScreen").fetchSemanticsNodes().isNotEmpty()
            }

            scenario.onActivity { activity ->
                assertTrue(activity.viewModel.sessionState.value != SessionState.SignedIn)
                assertEquals(emptyList<Any>(), activity.viewModel.deepLink.value)
            }
        }
    }

    @Test
    fun `a product link followed while signed out waits for the session`() {
        ActivityScenario.launch<MainActivity>(launchIntent("product/coffee")).use { scenario ->
            val activity = scenario.awaitUntil {
                it.viewModel.sessionState.value != SessionState.Unknown && it.viewModel.deepLink.value.isNotEmpty()
            }
            shadowOf(Looper.getMainLooper()).idle()

            assertTrue(activity.viewModel.sessionState.value != SessionState.SignedIn)
            assertTrue(activity.viewModel.deepLink.value.isNotEmpty())
        }
    }

    private companion object {

        const val SETTLE_STEP_MS = 20L
        const val WAIT_MS = 5_000L
    }
}
