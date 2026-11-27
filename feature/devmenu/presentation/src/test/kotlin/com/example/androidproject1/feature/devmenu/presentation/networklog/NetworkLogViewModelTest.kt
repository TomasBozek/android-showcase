package com.example.androidproject1.feature.devmenu.presentation.networklog

import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.test.MainDispatcherRule
import com.example.androidproject1.service.devtools.NetworkLogBuffer
import com.example.androidproject1.service.log.domain.test.FakeLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NetworkLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val buffer = NetworkLogBuffer()

    private fun record(path: String, status: Int? = 200) = buffer.record(
        method = "GET",
        url = "https://example.test$path",
        status = status,
        durationMs = 40,
        requestBody = null,
        responseBody = """{"ok":true}""",
    )

    private fun viewModel() = NetworkLogViewModel(networkLog = buffer, logger = FakeLogger())

    @Test
    fun `the log is the buffer's, newest first, and follows it`() = runTest {
        record("/a")
        val viewModel = viewModel()
        record("/b")

        assertEquals(listOf("/b", "/a"), viewModel.state.value.data?.entries?.map { it.path })
        assertNull(viewModel.state.value.loading)
    }

    @Test
    fun `a tap opens the exchange, and dismissing closes it`() = runTest {
        record("/a")
        val viewModel = viewModel()
        val id = viewModel.state.value.data!!.entries.single().id

        viewModel.onUiEvent(NetworkLogEvent.EntryClicked(id))
        assertEquals("/a", viewModel.state.value.data?.selected?.path)

        viewModel.onUiEvent(NetworkLogEvent.DetailDismissed)
        assertNull(viewModel.state.value.data?.selected)
    }

    @Test
    fun `copy hands the open exchange to the clipboard`() = runTest {
        record("/a", status = 404)
        val viewModel = viewModel()
        viewModel.onUiEvent(NetworkLogEvent.EntryClicked(viewModel.state.value.data!!.entries.single().id))

        viewModel.onUiEvent(NetworkLogEvent.CopyClicked)

        val copied = (viewModel.command.first() as UiCommand.Copy).text
        assertTrue(copied, "GET https://example.test/a" in copied && "404" in copied && """{"ok":true}""" in copied)
    }

    @Test
    fun `clear empties the log`() = runTest {
        record("/a")
        val viewModel = viewModel()

        viewModel.onUiEvent(NetworkLogEvent.ClearClicked)

        assertTrue(viewModel.state.value.data!!.entries.isEmpty())
    }

    @Test
    fun `up goes back`() = runTest {
        val viewModel = viewModel()

        viewModel.onUiEvent(NetworkLogEvent.UpClicked)

        assertEquals(NetworkLogNavigation.NavigateUp, viewModel.navigation.first())
    }
}
