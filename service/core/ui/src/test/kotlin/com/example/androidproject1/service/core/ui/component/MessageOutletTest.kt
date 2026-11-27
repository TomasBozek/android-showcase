package com.example.androidproject1.service.core.ui.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.androidproject1.service.core.ui.event.AppMessage
import com.example.androidproject1.service.core.ui.event.MessageAction
import com.example.androidproject1.service.core.ui.event.MessageActionHandler
import com.example.androidproject1.service.core.ui.event.MessageDuration
import com.example.androidproject1.service.core.ui.event.MessageOutlet
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.ToastTone
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.event.UiEvent
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.test.FakeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The outlet: posted from anywhere, shown by whichever `Screen()` is up, outliving it. */
@RunWith(RobolectricTestRunner::class)
class MessageOutletTest {

    @get:Rule
    val compose = createComposeRule()

    private sealed interface TestEvent : UiEvent

    private class TestViewModel : BaseViewModel<String, TestEvent, Nothing>(
        initialState = "body",
        logger = FakeLogger(),
    ) {

        val replies = mutableListOf<String>()

        fun send(command: UiCommand) = sendCommand(command)

        override fun onSystemEvent(event: SystemEvent) {
            if (event is SystemEvent.SnackbarAction) replies += event.id else super.onSystemEvent(event)
        }
    }

    private val outlet = MessageOutlet(scope = CoroutineScope(Dispatchers.Unconfined))

    private var shown by mutableStateOf(true)

    /** What the chrome was asked to draw: the default's, with every message's visuals kept. */
    private val drawn = mutableListOf<MessageVisuals>()

    private val recordingChrome = object : ScreenChrome by DefaultScreenChrome {
        @Composable
        override fun SnackbarHost(hostState: SnackbarHostState, modifier: Modifier) {
            hostState.currentSnackbarData?.visuals?.let { visuals ->
                SideEffect { if (drawn.lastOrNull() !== visuals) drawn += visuals as MessageVisuals }
            }
            DefaultScreenChrome.SnackbarHost(hostState, modifier)
        }
    }

    private fun screen(viewModel: TestViewModel = TestViewModel()) {
        compose.setContent {
            if (shown) {
                CompositionLocalProvider(LocalScreenChrome provides recordingChrome) {
                    ProvideMessageOutlet(outlet) {
                        Screen(viewModel = viewModel) { state, _ -> Text(state) }
                    }
                }
            }
        }
    }

    @Test
    fun `a message posted from outside any view model is shown`() {
        screen()

        outlet.post(AppMessage(key = "saved", text = "Saved".toUiText()))

        compose.onNodeWithText("Saved").assertIsDisplayed()
    }

    @Test
    fun `a message outlives the screen it was shown on`() {
        screen()
        outlet.post(AppMessage(key = "removed", text = "Removed".toUiText(), duration = MessageDuration.Indefinite))
        compose.onNodeWithText("Removed").assertIsDisplayed()

        shown = false
        compose.waitForIdle()
        assertEquals(listOf("removed"), outlet.pending.value.map { it.key })

        shown = true
        compose.onNodeWithText("Removed").assertIsDisplayed()
    }

    @Test
    fun `a message read and then left is not shown again on the next screen`() {
        screen()
        outlet.post(AppMessage(key = "favourite", text = "Added to favourites".toUiText()))
        compose.onNodeWithText("Added to favourites").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(TWO_SECONDS_MILLIS)

        shown = false
        compose.waitForIdle()
        assertEquals(emptyList<AppMessage>(), outlet.pending.value)

        shown = true
        compose.onNodeWithText("Added to favourites").assertDoesNotExist()
    }

    @Test
    fun `a message cut short before it could be read is shown once on the next screen`() {
        screen()
        outlet.post(AppMessage(key = "saved", text = "Saved".toUiText()))
        compose.onNodeWithText("Saved").assertIsDisplayed()

        shown = false
        compose.waitForIdle()
        assertEquals(listOf("saved"), outlet.pending.value.map { it.key })

        shown = true
        compose.onNodeWithText("Saved").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(TWO_SECONDS_MILLIS)

        shown = false
        compose.waitForIdle()
        assertEquals(emptyList<AppMessage>(), outlet.pending.value)
    }

    @Test
    fun `a message that waits to be dismissed is carried until it is`() {
        screen()
        outlet.post(AppMessage(key = "failed", text = "Couldn't save".toUiText(), tone = ToastTone.Error))
        compose.onNodeWithText("Couldn't save").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(TWO_SECONDS_MILLIS)

        shown = false
        compose.waitForIdle()
        assertEquals(listOf("failed"), outlet.pending.value.map { it.key })

        shown = true
        compose.onNodeWithText("Couldn't save").assertIsDisplayed()
    }

    @Test
    fun `a key posted twice is shown once`() {
        shown = false
        screen()
        outlet.post(AppMessage(key = "sync", text = "Syncing".toUiText()))
        outlet.post(AppMessage(key = "sync", text = "Synced".toUiText()))

        shown = true

        compose.onNodeWithText("Synced").assertIsDisplayed()
        compose.onNodeWithText("Syncing").assertDoesNotExist()
        assertEquals(1, outlet.pending.value.size)
    }

    @Test
    fun `a high priority message goes ahead of the normal ones`() {
        outlet.post(AppMessage(key = "a", text = "a".toUiText()))
        outlet.post(AppMessage(key = "b", text = "b".toUiText()))
        outlet.post(AppMessage(key = "urgent", text = "urgent".toUiText(), priority = AppMessage.Priority.High))

        assertEquals(listOf("urgent", "a", "b"), outlet.pending.value.map { it.key })
    }

    @Test
    fun `a handler action calls its bound handler with the argument`() {
        val handled = mutableListOf<String>()
        val restore = object : MessageActionHandler {
            override val key = "inventory.restore"
            override suspend fun handle(argument: String) {
                handled += argument
            }
        }
        outlet.bindHandlers(FakeLogger()) { key -> restore.takeIf { it.key == key } }
        screen()

        outlet.post(
            AppMessage(
                key = "deleted",
                text = "Deleted".toUiText(),
                action = MessageAction.Handler("Undo".toUiText(), handlerKey = "inventory.restore", argument = "42"),
                duration = MessageDuration.Indefinite,
            ),
        )
        compose.onNodeWithText("Undo").performClick()
        compose.waitForIdle()

        assertEquals(listOf("42"), handled)
        assertEquals(emptyList<AppMessage>(), outlet.pending.value)
    }

    // --- tone ---

    @Test
    fun `a message with no action is a toast in its tone, gone in a few seconds`() {
        screen()

        outlet.post(AppMessage(key = "saved", text = "Saved".toUiText(), tone = ToastTone.Success))
        compose.onNodeWithText("Saved").assertIsDisplayed()

        val toast = drawn.single()
        assertEquals(ToastTone.Success, toast.tone)
        assertEquals(SnackbarDuration.Short, toast.duration)
        assertEquals(MessageKind.Toast, toast.kind)
    }

    @Test
    fun `a message with an action is a snackbar, and waits a little longer`() {
        screen()

        outlet.post(
            AppMessage(
                key = "removed",
                text = "Removed".toUiText(),
                action = MessageAction.Command("Undo".toUiText(), UiCommand.NavigateBack),
                tone = ToastTone.Success,
            ),
        )
        compose.onNodeWithText("Removed").assertIsDisplayed()

        val snackbar = drawn.single()
        assertEquals(MessageKind.Snackbar, snackbar.kind)
        assertEquals(SnackbarDuration.Long, snackbar.duration)
    }

    @Test
    fun `an error toast waits to be read, and can be dismissed`() {
        screen()

        outlet.post(AppMessage(key = "failed", text = "Couldn't save".toUiText(), tone = ToastTone.Error))
        compose.mainClock.advanceTimeBy(ONE_MINUTE_MILLIS)

        compose.onNodeWithText("Couldn't save").assertIsDisplayed()
        val toast = drawn.single()
        assertEquals(SnackbarDuration.Indefinite, toast.duration)
        assertEquals(true, toast.withDismissAction)
    }

    @Test
    fun `a view model's toast carries its tone`() {
        val viewModel = TestViewModel()
        screen(viewModel)

        viewModel.send(UiCommand.ShowSnackbar(id = "saved", message = "Saved".toUiText(), tone = ToastTone.Success))
        compose.onNodeWithText("Saved").assertIsDisplayed()

        assertEquals(ToastTone.Success, drawn.single().tone)
    }

    @Test
    fun `a view model's snackbar is posted, and its action comes back to it`() {
        val viewModel = TestViewModel()
        screen(viewModel)

        viewModel.send(
            UiCommand.ShowSnackbar(id = "undo", message = "Removed".toUiText(), actionLabel = "Undo".toUiText()),
        )
        compose.onNodeWithText("Undo").performClick()
        compose.waitForIdle()

        assertEquals(listOf("undo"), viewModel.replies)
    }

    private companion object {
        const val ONE_MINUTE_MILLIS = 60_000L
        const val TWO_SECONDS_MILLIS = 2_000L
    }
}
