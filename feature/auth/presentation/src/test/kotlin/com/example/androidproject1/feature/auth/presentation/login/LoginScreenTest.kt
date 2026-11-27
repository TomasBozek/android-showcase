package com.example.androidproject1.feature.auth.presentation.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.service.core.ui.test.assertAccessible
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The pattern for testing a screen.
 *
 * A ViewModel test says what the state becomes; this says what is on screen and what a tap does,
 * which is the half a ViewModel test cannot reach. It runs under Robolectric as an ordinary unit
 * test, so `./gradlew test` covers it and CI needs no emulator.
 *
 * **Everything is found by `testTag`, never by text.** Copy gets reworded and translated; a test
 * that finds a button by its label fails on a wording change that broke nothing. The tags are the
 * ones from the screen, following `<screenStem>_<element>`.
 */
@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<LoginEvent>()

    private fun render(state: LoginState) {
        compose.setContent {
            AppTheme {
                LoginScreen(state = state, onEvent = events::add)
            }
        }
    }

    @Test
    fun `renders the fields and the submit button`() {
        render(LoginState.PREVIEW)

        compose.onNodeWithTag("login_emailField").assertIsDisplayed()
        compose.onNodeWithTag("login_passwordField").assertIsDisplayed()
        compose.onNodeWithTag("login_submitButton").assertIsDisplayed()
    }

    @Test
    fun `submit stays enabled on an empty form, because pressing it is what says what is wrong`() {
        // A disabled button can say *whether* the form is submittable and never *why*.
        render(LoginState.EMPTY)

        compose.onNodeWithTag("login_submitButton").assertIsEnabled()
    }

    @Test
    fun `typing an email reports it as an event`() {
        render(LoginState.EMPTY)

        // AppTextField is a label, an input and a supporting line, and the tag is on the group —
        // the Modifier convention puts a caller's modifier on the outermost element. So a test
        // that types reaches the input inside it. This is the pattern for any compound component;
        // assertions about the group itself use the tag directly, as the tests above do.
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("login_emailField")))
            .performTextInput("a")

        assertEquals(listOf(LoginEvent.EmailChanged("a")), events)
    }

    @Test
    fun `pressing submit reports it as an event`() {
        render(LoginState.PREVIEW)

        compose.onNodeWithTag("login_submitButton").performClick()

        assertEquals(listOf(LoginEvent.LoginClicked), events)
    }

    @Test
    fun `submit on an empty form still reports, so the view model can say what is wrong`() {
        // The tap is what reveals the errors, so an invalid form has to let it through.
        render(LoginState.EMPTY)

        compose.onNodeWithTag("login_submitButton").performClick()

        assertEquals(listOf(LoginEvent.LoginClicked), events)
    }

    @Test
    fun `is accessible`() {
        // Every clickable has a name and a touch target the theme allows — the two facts a
        // screen reader and a thumb need, on the state the previews show.
        render(LoginState.PREVIEW)

        compose.assertAccessible()
    }

    @Test
    fun `the submit button can be scrolled to`() {
        render(LoginState.PREVIEW)

        // performScrollTo throws when nothing above the node scrolls, which is what this form was:
        // a fixed centred column whose submit button is off-screen at a large font or under an
        // open keyboard.
        compose.onNodeWithTag("login_submitButton").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `forgot password reports its event`() {
        render(LoginState.EMPTY)

        compose.onNodeWithTag("login_forgotPasswordButton").performScrollTo().performClick()

        assertEquals(listOf(LoginEvent.ForgotPasswordClicked), events)
    }

    @Test
    fun `skip login is found by id and reports the event`() {
        render(LoginState.EMPTY)

        compose.onNodeWithTag("login_skipButton").performScrollTo().performClick()

        assertEquals(listOf(LoginEvent.SkipLoginClicked), events)
    }
}
