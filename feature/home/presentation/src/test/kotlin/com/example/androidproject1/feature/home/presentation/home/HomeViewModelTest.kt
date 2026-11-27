package com.example.androidproject1.feature.home.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.core.ui.shell.ShellCreation
import com.example.androidproject1.core.ui.shell.ShellCreations
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellDoors
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.feature.auth.domain.Session
import com.example.androidproject1.feature.auth.domain.test.FakeAuthService
import com.example.androidproject1.feature.home.presentation.R
import com.example.androidproject1.feature.profile.domain.Profile
import com.example.androidproject1.feature.profile.domain.test.FakeProfileRepository
import com.example.androidproject1.service.core.ui.test.MainDispatcherRule
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.log.domain.test.FakeLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Home on doors a test contributes: it names no feature, so none is faked here. */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profile = FakeProfileRepository()
    private val auth = FakeAuthService()

    private val shopFigure = MutableStateFlow<UiText?>(null)
    private val shopBadge = MutableStateFlow(0)

    private fun door(
        id: String,
        section: ShellSection,
        order: Int,
        placements: Set<ShellPlacement> = setOf(ShellPlacement.Home),
        figure: MutableStateFlow<UiText?> = MutableStateFlow("$id figure".toUiText()),
        badge: MutableStateFlow<Int> = MutableStateFlow(0),
    ) = ShellDoor(
        id = id,
        section = section,
        title = 0,
        icon = Icons.Filled.Star,
        route = Route(id),
        placements = placements,
        order = order,
        figure = figure,
        badge = badge,
    )

    // Out of order, and one placed in the menu only: Home sorts, and draws only its own.
    private val doors = listOf(
        door("settings", ShellSection.App, order = 2),
        door("menuOnly", ShellSection.Yours, order = 1, placements = setOf(ShellPlacement.Menu)),
        door("shop", ShellSection.Shop, order = 1, figure = shopFigure, badge = shopBadge),
        door("profile", ShellSection.App, order = 1),
        door("devMenu", ShellSection.Developer, order = 1, figure = MutableStateFlow(null)),
    )

    // Out of order too, and one minting a fresh key on each press: the dial sorts, and asks every time.
    private var minted = 0
    private val creations = listOf(
        ShellCreation("place", label = 0, icon = Icons.Filled.Star, testTag = "home_addPlaceItem", order = 2) {
            Route("place")
        },
        ShellCreation("item", label = 0, icon = Icons.Filled.Star, testTag = "home_newItemItem", order = 1) {
            Route("item${++minted}")
        },
    )

    private fun viewModel(creations: List<ShellCreation> = this.creations) = HomeViewModel(
        logger = FakeLogger(),
        doors = ShellDoors(doors),
        creations = ShellCreations(creations),
        profileRepository = profile,
        authService = auth,
    )

    @Test
    fun `renders immediately, with no loading overlay`() = runTest {
        val state = viewModel().state.value

        assertEquals(R.string.home_greeting.toUiText(), state.data?.greeting)
        assertNull(state.loading)
    }

    @Test
    fun `doors arrive from the contributions in section and order`() = runTest {
        val ids = viewModel().state.value.data?.doors?.map { it.id }

        assertEquals(listOf("shop", "profile", "settings", "devMenu"), ids)
    }

    @Test
    fun `a figure fills in when its flow emits, and a badge follows its count`() = runTest {
        val viewModel = viewModel()
        assertNull(viewModel.state.value.data?.door("shop")?.figure)
        assertFalse(viewModel.state.value.data!!.figuresIn)

        shopFigure.value = "4 categories".toUiText()
        shopBadge.value = 3

        val shop = viewModel.state.value.data?.door("shop")
        assertEquals("4 categories".toUiText(), shop?.figure)
        assertEquals(3, shop?.badge)
        // The developer door has nothing to count and is not waited for.
        assertTrue(viewModel.state.value.data!!.figuresIn)
    }

    @Test
    fun `a door click navigates to its route`() = runTest {
        val viewModel = viewModel()

        viewModel.onUiEvent(HomeEvent.DoorClicked("profile"))

        assertEquals(HomeNavigation.Open(Route("profile")), viewModel.navigation.first())
    }

    @Test
    fun `the greeting prefers the profile's name, then the email`() = runTest {
        val viewModel = viewModel()
        assertEquals(R.string.home_greeting.toUiText(), viewModel.state.value.data?.greeting)

        auth.session.value = Session(id = "session-1", email = "ada@example.com")
        assertEquals(R.string.home_greeting_named.toUiText("ada@example.com"), viewModel.state.value.data?.greeting)

        profile.save(Profile(name = "Ada", email = "ada@example.com", avatarUri = null))
        assertEquals(R.string.home_greeting_named.toUiText("Ada"), viewModel.state.value.data?.greeting)
    }

    @Test
    fun `the speed dial's toggle is state, and a choice navigates and closes it`() = runTest {
        val viewModel = viewModel()
        assertEquals(listOf("item", "place"), viewModel.state.value.data?.creations?.map { it.id })

        viewModel.onUiEvent(HomeEvent.CreateToggled(true))
        assertEquals(true, viewModel.state.value.data?.createOpen)

        listOf("item" to Route("item1"), "place" to Route("place"), "item" to Route("item2")).forEach { (id, route) ->
            viewModel.onUiEvent(HomeEvent.CreateToggled(true))
            viewModel.onUiEvent(HomeEvent.CreateChosen(id))
            assertEquals(HomeNavigation.Create(route), viewModel.navigation.first())
            assertEquals(false, viewModel.state.value.data?.createOpen)
        }
    }

    @Test
    fun `no creation hides the dial`() = runTest {
        val viewModel = viewModel(creations = emptyList())

        assertEquals(false, viewModel.state.value.data?.canCreate)
        assertEquals(true, viewModel().state.value.data?.canCreate)
    }

    @Test
    fun `the developer doors are what the host reports`() = runTest {
        val viewModel = viewModel()
        assertEquals(false, viewModel.state.value.data?.developerDoors)

        viewModel.onUiEvent(HomeEvent.DeveloperDoorsAvailable(true))

        assertEquals(true, viewModel.state.value.data?.developerDoors)
    }
}

private data class Route(val id: String) : NavKey
