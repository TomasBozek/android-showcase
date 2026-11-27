package com.example.androidproject1

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.feature.inventory.api.InventoryEditorDestination
import com.example.androidproject1.feature.trips.api.TripWizardDestination
import com.example.androidproject1.feature.weather.api.WeatherDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import java.io.File

/**
 * The launcher's three shortcuts: `res/xml/shortcuts.xml` parsed as the launcher reads it,
 * each intent's link checked against this build's scheme and resolved by [DeepLinks].
 *
 * A shortcut's intent cannot use `${applicationId}`, so each flavor has its own copy of the file;
 * the last case holds the three to one text with only the scheme different.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application: `App` would start the global Koin, which another test in the sandbox owns.
@Config(application = Application::class)
class ShortcutsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private data class Shortcut(val id: String, val action: String?, val data: String?)

    private fun shortcuts(): List<Shortcut> {
        val parser = context.resources.getXml(R.xml.shortcuts)
        val found = mutableListOf<Shortcut>()
        var id: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "shortcut" -> id = parser.getAttributeValue(ANDROID, "shortcutId")
                "intent" -> found += Shortcut(
                    id = id!!,
                    action = parser.getAttributeValue(ANDROID, "action"),
                    data = parser.getAttributeValue(ANDROID, "data"),
                )
            }
        }
        return found
    }

    @Test
    fun `three shortcuts, each a link into this build that resolves to its create screen`() {
        val shortcuts = shortcuts()

        assertEquals(listOf("newTrip", "newItem", "addPlace"), shortcuts.map { it.id })
        shortcuts.forEach {
            assertEquals("android.intent.action.VIEW", it.action)
            assertTrue("${it.data} is this build's scheme", it.data!!.startsWith("${BuildConfig.APPLICATION_ID}://"))
        }
        val targets = shortcuts.map { DeepLinks.parse(it.data)!!.keys.last() }
        assertEquals(TripWizardDestination, targets[0])
        assertTrue(targets[1] is InventoryEditorDestination)
        assertEquals(WeatherDestination, targets[2])
    }

    @Test
    fun `the flavors' copies differ in their scheme and nothing else`() {
        val base = BuildConfig.APPLICATION_ID.removeSuffix(".dev")
        val main = File("src/main/res/xml/shortcuts.xml").readText()
        listOf("dev", "staging").forEach { flavor ->
            val copy = File("src/$flavor/res/xml/shortcuts.xml").readText()
            assertEquals(
                flavor,
                main,
                copy.replace("$base.$flavor://", "$base://").replace("$base.$flavor\"", "$base\""),
            )
        }
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
