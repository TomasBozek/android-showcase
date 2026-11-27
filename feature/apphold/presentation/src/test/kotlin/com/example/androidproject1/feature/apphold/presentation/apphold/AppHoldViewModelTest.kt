package com.example.androidproject1.feature.apphold.presentation.apphold

import com.example.androidproject1.feature.apphold.api.AppHoldDestination
import com.example.androidproject1.feature.apphold.api.AppHoldReason
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.test.MainDispatcherRule
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.play.UpdateKind
import com.example.androidproject1.service.play.UpdateOffer
import com.example.androidproject1.service.play.UpdateStart
import com.example.androidproject1.service.play.test.FakeAppUpdates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppHoldViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(reason: AppHoldReason) = AppHoldViewModel(
        logger = FakeLogger(),
        args = AppHoldDestination(reason = reason, storeUrl = STORE_URL),
        appUpdates = updates,
    )

    private val updates = FakeAppUpdates()

    private fun offer(immediate: Boolean = true) =
        UpdateOffer(versionCode = 9, staleDays = 3, priority = 5, immediateAllowed = immediate, flexibleAllowed = true)

    @Test
    fun `the screen draws the reason the route carries`() {
        assertEquals(AppHoldReason.Maintenance, viewModel(AppHoldReason.Maintenance).state.value.data?.reason)
    }

    @Test
    fun `update starts Play's immediate flow and does not open the store`() = runTest {
        updates.offer = offer()
        val viewModel = viewModel(AppHoldReason.UpdateRequired)

        viewModel.onUiEvent(AppHoldEvent.UpdateClicked)

        assertEquals(listOf(UpdateKind.Immediate), updates.started)
        assertNull(viewModel.state.value.alert)
        assertEquals(false, viewModel.commandPending())
    }

    @Test
    fun `with no offer from Play, update opens this app's store page`() = runTest {
        val viewModel = viewModel(AppHoldReason.UpdateRequired)

        viewModel.onUiEvent(AppHoldEvent.UpdateClicked)

        assertTrue(updates.started.isEmpty())
        assertEquals(UiCommand.OpenBrowser(STORE_URL), viewModel.command.first())
    }

    @Test
    fun `an update Play will not do in place falls back to the store`() = runTest {
        updates.offer = offer(immediate = false)
        val viewModel = viewModel(AppHoldReason.UpdateRequired)

        viewModel.onUiEvent(AppHoldEvent.UpdateClicked)

        assertEquals(UiCommand.OpenBrowser(STORE_URL), viewModel.command.first())
    }

    @Test
    fun `a user who declines Play's flow stays where they are, and is not sent to the store`() = runTest {
        updates.offer = offer()
        updates.startResult = UpdateStart.Declined
        val viewModel = viewModel(AppHoldReason.UpdateRequired)

        viewModel.onUiEvent(AppHoldEvent.UpdateClicked)

        assertEquals(listOf(UpdateKind.Immediate), updates.started)
        assertEquals(false, viewModel.commandPending())
    }

    /** Whether a command was sent: what arrives at once is there, and nothing else will. */
    private suspend fun AppHoldViewModel.commandPending() = withTimeoutOrNull(50) { command.first() } != null

    private companion object {

        const val STORE_URL = "https://play.google.com/store/apps/details?id=app.test"
    }
}
