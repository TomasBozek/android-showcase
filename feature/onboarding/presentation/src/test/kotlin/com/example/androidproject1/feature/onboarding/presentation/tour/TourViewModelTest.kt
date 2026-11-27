package com.example.androidproject1.feature.onboarding.presentation.tour

import com.example.androidproject1.feature.onboarding.domain.test.FakeOnboardingRepository
import com.example.androidproject1.service.core.ui.test.MainDispatcherRule
import com.example.androidproject1.service.log.domain.test.FakeLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The tour is a plain view model over one flag, so what is worth asserting is when it
 * runs, when it stops, and that it never runs twice without being asked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TourViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeOnboardingRepository()

    private fun viewModel() = TourViewModel(logger = FakeLogger(), onboardingRepository = repository)

    @Test
    fun `it starts on an unseen tour and not on a seen one`() = runTest {
        val first = viewModel()
        first.start(STEPS)
        advanceUntilIdle()
        assertEquals(0, first.state.value.step)
        assertEquals(STEPS, first.state.value.steps)

        repository.tourSeen.value = true
        val second = viewModel()
        second.start(STEPS)
        advanceUntilIdle()
        assertNull("a tour someone has already walked does not come back", second.state.value.step)
    }

    @Test
    fun `Next walks to the end, and the last one marks it seen`() = runTest {
        val viewModel = viewModel()
        viewModel.start(STEPS)
        advanceUntilIdle()

        viewModel.onEvent(TourEvent.Next)
        assertEquals(1, viewModel.state.value.step)
        assertFalse("not yet — there is a step left", repository.tourSeen.value)

        viewModel.onEvent(TourEvent.Next)
        assertEquals(2, viewModel.state.value.step)

        viewModel.onEvent(TourEvent.Next)
        advanceUntilIdle()
        assertNull("the tour is over", viewModel.state.value.step)
        assertTrue(repository.tourSeen.value)
    }

    @Test
    fun `Skip ends it and marks it seen, so it is not offered twice`() = runTest {
        val viewModel = viewModel()
        viewModel.start(STEPS)
        advanceUntilIdle()

        viewModel.onEvent(TourEvent.Skip)
        advanceUntilIdle()

        assertNull(viewModel.state.value.step)
        assertTrue(repository.tourSeen.value)
    }

    @Test
    fun `a reset replays it the next time the host starts one`() = runTest {
        val viewModel = viewModel()
        viewModel.start(STEPS)
        advanceUntilIdle()
        viewModel.onEvent(TourEvent.Skip)
        advanceUntilIdle()

        // What the Settings row and the drawer row do: the flag goes back, and the next Home
        // starts it again.
        repository.resetTour()
        viewModel.start(STEPS)
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.step)
    }

    @Test
    fun `a link pauses the tour without marking it seen, and a later start does not bring it back`() = runTest {
        // A link opened over the tour is somewhere the user asked to go: the tour steps aside
        // rather than dimming the screen it lands on. It is not walked, so it stays unseen
        // and is offered on the next launch.
        val viewModel = viewModel()
        viewModel.start(STEPS)
        advanceUntilIdle()

        viewModel.pause()
        advanceUntilIdle()
        assertNull(viewModel.state.value.step)
        assertFalse(repository.tourSeen.value)

        viewModel.start(STEPS.reversed())
        advanceUntilIdle()
        assertNull("paused for this launch", viewModel.state.value.step)

        viewModel.replay()
        advanceUntilIdle()
        assertEquals("a replay asked for by name ends the pause", 0, viewModel.state.value.step)
    }

    @Test
    fun `starting an already running tour does not put it back to the first step`() = runTest {
        val viewModel = viewModel()
        viewModel.start(STEPS)
        advanceUntilIdle()
        viewModel.onEvent(TourEvent.Next)

        // Home recomposing, or a rotation: the host calls start again, and a tour that jumped
        // back to step one each time would never be walkable.
        viewModel.start(STEPS)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.step)
    }

    private companion object {

        val STEPS = listOf(
            TourStep("home_catalogCard", "The shop", "Everything for sale."),
            TourStep("menu_openButton", "The menu", "Everywhere else."),
            TourStep("tabs_settingsTab", "Settings", "The theme and the way out."),
        )
    }
}
