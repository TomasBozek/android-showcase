package com.example.androidproject1.service.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiEvent
import com.example.androidproject1.service.core.ui.form.required
import com.example.androidproject1.service.core.ui.state.AlertAnswer
import com.example.androidproject1.service.core.ui.state.AlertInput
import com.example.androidproject1.service.core.ui.state.AlertOption
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.test.FakeLogger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** An alert that asks for a word or a choice: raised like any other, answered on Confirmed. */
@RunWith(RobolectricTestRunner::class)
class AlertInputTest {

    @get:Rule
    val compose = createComposeRule()

    private sealed interface TestEvent : UiEvent

    private class TestViewModel : BaseViewModel<String, TestEvent, Nothing>(
        initialState = "body",
        logger = FakeLogger(),
    ) {

        val results = mutableListOf<SystemEvent.AlertResult>()

        fun ask(input: AlertInput) = uiState.setAlert(
            id = "ask",
            title = "Name this list".toUiText(),
            message = "Only you see it.".toUiText(),
            confirmLabel = "Save".toUiText(),
            input = input,
        )

        override fun onSystemEvent(event: SystemEvent) {
            if (event is SystemEvent.AlertResult) results += event
            super.onSystemEvent(event)
        }
    }

    private val viewModel = TestViewModel()

    private val options = listOf(
        AlertOption("groceries", "Groceries".toUiText()),
        AlertOption("hardware", "Hardware".toUiText()),
        AlertOption("gifts", "Gifts".toUiText()),
    )

    private fun screen() = compose.setContent {
        Screen(viewModel = viewModel) { state, _ -> Text(state) }
    }

    private val confirm get() = compose.onNodeWithTag(ALERT_CONFIRM_TAG)

    /** The tag is on the field, or on the group around it: a chrome decides which. */
    private val answerField
        get() = compose.onNode(
            hasSetTextAction() and (hasTestTag(ALERT_ANSWER_TAG) or hasAnyAncestor(hasTestTag(ALERT_ANSWER_TAG))),
        )

    @Test
    fun `a text alert returns what was typed`() {
        screen()
        viewModel.ask(AlertInput.Text(label = "Name".toUiText(), initial = "Weekend"))

        answerField.performTextReplacement("Party")
        confirm.performClick()

        assertEquals(
            SystemEvent.AlertResult.Confirmed("ask", answer = AlertAnswer.Text("Party")),
            viewModel.results.single(),
        )
    }

    @Test
    fun `a text alert waits for its validator`() {
        screen()
        viewModel.ask(AlertInput.Text(label = "Name".toUiText(), validator = required()))

        confirm.assertIsNotEnabled()
    }

    @Test
    fun `a single choice returns the one picked`() {
        screen()
        viewModel.ask(AlertInput.Choice(options))

        compose.onNodeWithTag(alertItemTag("hardware")).performClick()
        confirm.performClick()

        assertEquals(
            SystemEvent.AlertResult.Confirmed("ask", answer = AlertAnswer.Choices(listOf("hardware"))),
            viewModel.results.single(),
        )
    }

    @Test
    fun `a multiple choice returns every one picked, in the options' order`() {
        screen()
        viewModel.ask(AlertInput.Choice(options, multiple = true, initial = setOf("gifts")))

        compose.onNodeWithTag(alertItemTag("groceries")).performClick()
        confirm.performClick()

        assertEquals(
            SystemEvent.AlertResult.Confirmed("ask", answer = AlertAnswer.Choices(listOf("groceries", "gifts"))),
            viewModel.results.single(),
        )
    }

    @Test
    fun `declining a question returns no answer`() {
        screen()
        viewModel.ask(input = AlertInput.Text()).also { viewModel.results.clear() }
        compose.onNodeWithTag(ALERT_DECLINE_TAG).performClick()

        assertEquals(SystemEvent.AlertResult.Declined("ask"), viewModel.results.single())
    }
}
