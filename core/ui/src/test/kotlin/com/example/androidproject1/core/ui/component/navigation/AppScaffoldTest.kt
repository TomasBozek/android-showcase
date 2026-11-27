package com.example.androidproject1.core.ui.component.navigation

import android.app.Activity
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.component.ComponentTest
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.SizeClass
import com.example.androidproject1.service.core.ui.component.ContentMessage
import com.example.androidproject1.service.core.ui.component.LocalContentMessage
import com.example.androidproject1.service.core.ui.state.ContentState
import com.example.androidproject1.service.core.ui.text.toUiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * The shell keeps the screen when the chrome has something to say.
 *
 * `Screen()` sets [LocalContentMessage] over a screen that has data; what is pinned here is what
 * the scaffold does with it: the message takes the content's place, and the top bar, the up arrow
 * and the screen id stay where they were — a screen that loaded nothing is still that screen.
 */
class AppScaffoldTest : ComponentTest() {

    /**
     * The scaffold at a size class, with something inside it that fills whatever width it is given
     * — so what the test measures is the column's width, not the text's.
     */
    private var inset: Dp = 0.dp

    private fun scaffoldAt(sizeClass: SizeClass, contentPadding: Boolean = true) {
        compose.setContent {
            AppTheme(sizeClass = sizeClass) {
                inset = AppTheme.spacing.inset.xl
                AppScaffold(screenId = "XScreen", contentPadding = contentPadding) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).testTag("x_bodyValue"))
                }
            }
        }
    }

    private fun widthOf(tag: String): Dp =
        with(compose.density) { compose.onNodeWithTag(tag).fetchSemanticsNode().size.width.toDp() }

    private fun windowWidth(): Dp =
        with(compose.density) { compose.onNodeWithTag("XScreen").fetchSemanticsNode().size.width.toDp() }

    @Test
    @Config(qualifiers = TABLET)
    fun `a phone gets the whole window`() {
        // The window is the tablet's, the size class is the phone's: the cap follows the class the
        // theme was handed, and Compact has none — so the content is the window, wide as it is.
        scaffoldAt(SizeClass.Compact, contentPadding = false)

        assertEquals(windowWidth(), widthOf("x_bodyValue"))
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `a wide window caps the content at the reading width`() {
        // The one thing the modifier order decides: `fillMaxSize()` before `widthIn` fills the
        // window and caps nothing, and no preview says which way round it went.
        scaffoldAt(SizeClass.Expanded, contentPadding = false)

        val window = windowWidth()
        val content = widthOf("x_bodyValue")
        assertTrue(
            "the test window is $window, which is not wider than the 840 dp cap — nothing is proved",
            window > 840.dp,
        )
        assertEquals(840.dp, content)
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `the cap is the column's width and the padding is its padding`() {
        // A list passes contentPadding = false and still gets a reading width: SettingsScreen is
        // one of the screens the 1280 dp window was widest on.
        scaffoldAt(SizeClass.Expanded, contentPadding = true)

        assertEquals(840.dp - inset * 2, widthOf("x_bodyValue"))
    }

    private fun scaffold(message: ContentMessage?) {
        themed {
            CompositionLocalProvider(LocalContentMessage provides message) {
                AppScaffold(
                    screenId = "XScreen",
                    topBar = { AppTopBar(title = "X", onNavigateUp = {}, navigateUpTestTag = "x_upButton") },
                ) {
                    AppText(text = "the body", modifier = Modifier.testTag("x_bodyValue"))
                }
            }
        }
    }

    @Test
    fun `a content message draws in place of the content and keeps the top bar`() {
        scaffold(ContentMessage(ContentState.Empty(message = "Nothing here".toUiText())) {})

        compose.onNodeWithTag("XScreen").assertIsDisplayed()
        compose.onNodeWithTag("x_upButton").assertIsDisplayed()
        compose.onNodeWithTag(CONTENT_MESSAGE_TAG).assertIsDisplayed()
        compose.onNodeWithTag("x_bodyValue").assertDoesNotExist()
    }

    @Test
    fun `no message means the content, and no message node`() {
        scaffold(null)

        compose.onNodeWithTag("x_bodyValue").assertIsDisplayed()
        compose.onNodeWithTag(CONTENT_MESSAGE_TAG).assertDoesNotExist()
    }

    @Test
    fun `the message's action is the one the chrome was handed`() {
        var actions = 0
        scaffold(
            ContentMessage(
                ContentState.Error(message = "Failed".toUiText(), actionLabel = "Retry".toUiText()),
            ) { actions++ },
        )

        compose.onNode(hasClickAction() and hasAnyAncestor(hasTestTag(CONTENT_MESSAGE_TAG))).performClick()

        assertEquals(1, actions)
    }

    // A screen that shows a card number or an account keeps it out of screenshots and the
    // recents thumbnail, and only while it is composed.
    private lateinit var window: Window

    private fun secureFlag(): Boolean = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0

    @Composable
    private fun CaptureWindow() {
        window = generateSequence(LocalContext.current) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().first().window
    }

    @Test
    fun `a secure screen sets FLAG_SECURE while it is composed and clears it after`() {
        var shown by mutableStateOf(true)
        compose.setContent {
            CaptureWindow()
            AppTheme { if (shown) AppScaffold(screenId = "XScreen", secure = true) {} }
        }
        assertTrue(secureFlag())

        shown = false
        compose.waitForIdle()

        assertFalse(secureFlag())
    }

    @Test
    fun `an ordinary screen leaves the flag alone`() {
        compose.setContent {
            CaptureWindow()
            AppTheme { AppScaffold(screenId = "XScreen") {} }
        }

        assertFalse(secureFlag())
    }

    @Test
    fun `two secure screens at once, as in a transition, keep it until the last one leaves`() {
        var first by mutableStateOf(true)
        var second by mutableStateOf(true)
        compose.setContent {
            CaptureWindow()
            AppTheme {
                if (first) AppScaffold(screenId = "AScreen", secure = true) {}
                if (second) AppScaffold(screenId = "BScreen", secure = true) {}
            }
        }

        first = false
        compose.waitForIdle()
        assertTrue(secureFlag())

        second = false
        compose.waitForIdle()
        assertFalse(secureFlag())
    }
}

/**
 * Wider than the 840 dp cap, so a test of the cap is a test of something. Robolectric's default
 * window is 320 dp, on which every one of these assertions would have passed by being vacuous.
 */
private const val TABLET = "w1280dp-h800dp-xhdpi"
