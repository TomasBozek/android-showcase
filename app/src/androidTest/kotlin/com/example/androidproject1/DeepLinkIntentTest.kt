package com.example.androidproject1

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A link from outside reaches `MainActivity` through the manifest's intent filter — which
 * Robolectric does not resolve the way the platform does — and the activity takes its path.
 */
@RunWith(AndroidJUnit4::class)
class DeepLinkIntentTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val link = Uri.parse("${BuildConfig.APPLICATION_ID}://product/coffee")

    @Test
    fun theAppsSchemeResolvesToMainActivity() {
        val intent = Intent(Intent.ACTION_VIEW, link).addCategory(Intent.CATEGORY_BROWSABLE)

        val handlers = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        assertEquals(
            listOf(MainActivity::class.java.name),
            handlers.filter { it.activityInfo.packageName == context.packageName }.map { it.activityInfo.name },
        )
    }

    @Test
    fun theActivityTakesTheLinksPath() {
        val intent = Intent(Intent.ACTION_VIEW, link).setPackage(context.packageName)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            // Looked up off the main thread, and applied only once there is a session; signed out,
            // as a fresh install is, the keys wait — which is what is asserted.
            val deadline = System.currentTimeMillis() + WAIT_MS
            var keys = emptyList<Any>()
            while (keys.isEmpty() && System.currentTimeMillis() < deadline) {
                scenario.onActivity { keys = it.viewModel.deepLink.value }
                Thread.sleep(STEP_MS)
            }
            assertTrue("the link's keys are pending: $keys", keys.isNotEmpty())
        }
    }

    private companion object {

        const val WAIT_MS = 10_000L
        const val STEP_MS = 50L
    }
}
