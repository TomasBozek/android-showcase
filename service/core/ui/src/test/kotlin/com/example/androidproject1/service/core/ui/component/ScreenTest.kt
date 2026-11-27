package com.example.androidproject1.service.core.ui.component

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.service.core.ui.R
import com.example.androidproject1.service.core.ui.event.ExternalTarget
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.event.UiEvent
import com.example.androidproject1.service.core.ui.state.ContentState
import com.example.androidproject1.service.core.ui.test.assertAccessible
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.navigation.AppNavigator
import com.example.androidproject1.service.navigation.LocalAppNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Where the chrome's message goes.
 *
 * Over a screen that has data, `Screen()` still composes the content and hands the message to it
 * through [LocalContentMessage], so the shell underneath can draw it in the content's place and
 * keep its top bar. Over no data there is no shell to keep, and the chrome draws the message
 * itself, full screen, as it always did.
 */
@RunWith(RobolectricTestRunner::class)
class ScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private data class TestState(val value: String)

    private sealed interface TestEvent : UiEvent

    private class TestViewModel(initialState: TestState?) :
        BaseViewModel<TestState, TestEvent, Nothing>(initialState = initialState, logger = FakeLogger()) {

        fun say(state: ContentState) = showContent(state)

        fun send(command: UiCommand) = sendCommand(command)
    }

    private val message = ContentState.Empty(message = "Nothing here".toUiText())

    /** The stateless screen: what a feature's `XScreen` is, reduced to the local it never reads itself. */
    private fun screen(viewModel: TestViewModel, seen: (ContentMessage?) -> Unit) {
        compose.setContent {
            Screen(viewModel = viewModel) { state, _ ->
                seen(LocalContentMessage.current)
                Text(text = state.value, modifier = Modifier.testTag("x_bodyValue"))
            }
        }
    }

    @Test
    fun `a message over loaded data still composes the content`() {
        val viewModel = TestViewModel(TestState("loaded"))
        viewModel.say(message)
        var seen: ContentMessage? = null

        screen(viewModel) { seen = it }

        // The content is composed, and it is handed the message rather than replaced by it.
        compose.onNodeWithTag("x_bodyValue").assertIsDisplayed()
        assertNotNull(seen)
        assertEquals(message, seen?.state)
    }

    @Test
    fun `a message over no data draws the chrome's`() {
        val viewModel = TestViewModel(initialState = null)
        viewModel.say(message)
        var composed = false

        screen(viewModel) { composed = true }

        // No data, no shell: the module's own chrome draws the message full screen.
        compose.onNodeWithText("Nothing here").assertIsDisplayed()
        compose.onNodeWithTag("x_bodyValue").assertDoesNotExist()
        assertEquals(false, composed)
    }

    @Test
    fun `is accessible`() {
        // The stock chrome's own key — the retry on a full-screen error — has a name and a touch
        // target. With no theme in the composition the fallback floor, 48 dp, is the bar.
        val viewModel = TestViewModel(initialState = null)
        viewModel.say(ContentState.Error(message = "It failed".toUiText()))

        screen(viewModel) {}

        compose.assertAccessible()
    }

    @Test
    fun `no message means the content sees none`() {
        var seen: ContentMessage? = ContentMessage(message) {}

        screen(TestViewModel(TestState("loaded"))) { seen = it }

        assertNull(seen)
    }

    // -- the commands that leave the app ---------------------------------------------

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Renders a screen, sends it one command, and returns what it started, if anything. */
    private fun started(command: UiCommand): Intent? {
        val viewModel = TestViewModel(TestState("loaded"))
        screen(viewModel) {}
        viewModel.send(command)
        compose.waitForIdle()
        return shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).nextStartedActivity
    }

    @Test
    fun `navigate opens its route through the app navigator the shell provides`() {
        val asked = mutableListOf<List<NavKey>>()
        val navigator = object : AppNavigator {
            override fun navigate(keys: List<NavKey>) {
                asked += keys
            }

            override fun open(deepLink: String) = Unit
        }
        val viewModel = TestViewModel(TestState("loaded"))
        compose.setContent {
            CompositionLocalProvider(LocalAppNavigator provides navigator) {
                Screen(viewModel = viewModel) { state, _ -> Text(text = state.value) }
            }
        }

        viewModel.send(UiCommand.Navigate(listOf(Destination)))
        compose.waitForIdle()

        assertEquals(listOf(listOf<NavKey>(Destination)), asked)
    }

    @Test
    fun `share opens the share sheet with the text and the app's own link`() {
        val chooser = started(UiCommand.Share(text = "Desk lamp", subject = "Desk lamp", appPath = "product/42"))

        assertEquals(Intent.ACTION_CHOOSER, chooser?.action)
        @Suppress("DEPRECATION")
        val send = chooser?.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertEquals(Intent.ACTION_SEND, send?.action)
        val text = send?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(text, text.startsWith("Desk lamp"))
        assertTrue(text, text.endsWith("${context.packageName}://product/42"))
        assertEquals("Desk lamp", send?.getStringExtra(Intent.EXTRA_SUBJECT))
    }

    @Test
    fun `copy puts the text on the clipboard`() {
        started(UiCommand.Copy("ada@example.com"))

        val clipboard = context.getSystemService(ClipboardManager::class.java)
        assertEquals("ada@example.com", clipboard.primaryClip?.getItemAt(0)?.text)
    }

    @Test
    fun `a phone number opens the dialler, never a call`() {
        val intent = started(UiCommand.OpenExternal(ExternalTarget.Phone("+420 123 456 789")))

        assertEquals(Intent.ACTION_DIAL, intent?.action)
        assertEquals(Uri.parse("tel:+420 123 456 789"), intent?.data)
    }

    @Test
    fun `an address opens a mail client`() {
        val intent =
            started(
                UiCommand.OpenExternal(
                    ExternalTarget.Email("help@example.com", subject = "Hello".toUiText(), body = "Version 1.0"),
                ),
            )

        assertEquals(Intent.ACTION_SENDTO, intent?.action)
        assertEquals("mailto", intent?.data?.scheme)
        val part = intent?.data?.schemeSpecificPart.orEmpty()
        assertEquals("help@example.com", part.substringBefore('?'))
        assertTrue(part, part.contains("subject=Hello") && part.contains("body=Version 1.0"))
    }

    @Test
    fun `a place opens a map by name`() {
        val intent = started(UiCommand.OpenExternal(ExternalTarget.Map("Lisbon, Portugal")))

        assertEquals(Intent.ACTION_VIEW, intent?.action)
        assertEquals("geo", intent?.data?.scheme)
        // `geo:` is an opaque URI, so the query is read off its scheme-specific part, decoded.
        assertEquals("0,0?q=Lisbon, Portugal", intent?.data?.schemeSpecificPart)
    }

    @Test
    fun `a calendar event is inserted all day, through its last day`() {
        val intent = started(
            UiCommand.OpenExternal(
                ExternalTarget.CalendarEvent(
                    title = "Summer in Lisbon",
                    start = LocalDate.of(2026, 7, 10),
                    endInclusive = LocalDate.of(2026, 7, 17),
                    location = "Lisbon, Portugal",
                ),
            ),
        )

        assertEquals(Intent.ACTION_INSERT, intent?.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, intent?.data)
        assertEquals("Summer in Lisbon", intent?.getStringExtra(CalendarContract.Events.TITLE))
        assertEquals(true, intent?.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))
        val day = { date: LocalDate -> date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
        assertEquals(day(LocalDate.of(2026, 7, 10)), intent?.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0))
        assertEquals(day(LocalDate.of(2026, 7, 18)), intent?.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0))
    }

    @Test
    fun `a web page opens in a Custom Tab`() {
        val intent = started(UiCommand.OpenBrowser("https://example.com/privacy"))

        assertEquals(Intent.ACTION_VIEW, intent?.action)
        assertEquals(Uri.parse("https://example.com/privacy"), intent?.data)
        // What makes a VIEW intent a Custom Tab rather than the browser proper.
        assertTrue(intent?.hasExtra("android.support.customtabs.extra.SESSION") == true)
    }

    @Test
    fun `nothing that can open it is said, not swallowed`() {
        shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).checkActivities(true)

        started(UiCommand.OpenExternal(ExternalTarget.Phone("123")))

        compose.onNodeWithText(context.getString(R.string.core_command_no_app)).assertIsDisplayed()
    }
}

private data object Destination : NavKey
