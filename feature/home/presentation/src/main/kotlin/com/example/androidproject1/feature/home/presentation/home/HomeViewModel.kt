package com.example.androidproject1.feature.home.presentation.home

import androidx.lifecycle.viewModelScope
import com.example.androidproject1.core.ui.shell.ShellCreations
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellDoors
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.home.presentation.HomeCreationState
import com.example.androidproject1.feature.home.presentation.HomeDoorState
import com.example.androidproject1.feature.home.presentation.R
import com.example.androidproject1.feature.profile.domain.ProfileRepository
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * The crossroads: the doors the features contribute, each with a figure from its own
 * domain, and a greeting by name once the profile has one. Home names no other feature; what is a
 * door here is whatever is bound as a [ShellDoor] placed on [ShellPlacement.Home], and what the speed
 * dial offers is whatever is bound as a `ShellCreation`.
 */
class HomeViewModel(
    logger: Logger,
    doors: ShellDoors,
    creations: ShellCreations,
    private val profileRepository: ProfileRepository,
    private val authService: AuthService,
) : BaseViewModel<HomeState, HomeEvent, HomeNavigation>(
    initialState = HomeState(
        greeting = R.string.home_greeting.toUiText(),
        doors = doors.all.onHome().map(HomeDoorState::of),
        creations = creations.all.sortedBy { it.order }.map(HomeCreationState::of),
    ),
    logger = logger.withTag("HomeViewModel"),
) {

    private val doors = doors.all.onHome()
    private val creations = creations.all

    // The two halves of the greeting, each from its own flow.
    private var profileName: String? = null
    private var email: String? = null

    init {
        observeDoors()
        observeWho()
    }

    override fun onUiEvent(event: HomeEvent) = when (event) {
        is HomeEvent.DoorClicked -> doors.firstOrNull {
            it.id == event.id
        }?.let { navigate(HomeNavigation.Open(it.route)) }
            ?: Unit
        is HomeEvent.CreateToggled -> updateData { copy(createOpen = event.open) }
        is HomeEvent.CreateChosen -> {
            updateData { copy(createOpen = false) }
            creations.firstOrNull { it.id == event.id }?.let { navigate(HomeNavigation.Create(it.route())) } ?: Unit
        }
        is HomeEvent.DeveloperDoorsAvailable -> updateData { copy(developerDoors = event.available) }
    }

    // Every figure and badge is silent: a count that cannot be read leaves its card as it was, not
    // a dialog over the whole of Home.
    private fun observeDoors() = doors.forEach { door ->
        viewModelScope.launch {
            door.figure.catch { logger.w(it) { "Door ${door.id}'s figure failed" } }
                .collect { figure -> setDoor(door.id) { copy(figure = figure) } }
        }
        viewModelScope.launch {
            door.badge.catch { logger.w(it) { "Door ${door.id}'s badge failed" } }
                .collect { badge -> setDoor(door.id) { copy(badge = badge) } }
        }
    }

    /** The name from the profile, the email from the session: the greeting prefers the name. */
    private fun observeWho() {
        observe(
            flow = { profileRepository.observeProfile() },
            errorDisplay = ErrorDisplay.Silent,
            onData = { profile ->
                profileName = profile.name.takeIf { it.isNotBlank() }
                updateData { copy(greeting = greeting()) }
            },
        )
        observe(
            flow = { authService.observeSession() },
            errorDisplay = ErrorDisplay.Silent,
            onData = { session ->
                email = session?.email
                updateData { copy(greeting = greeting()) }
            },
        )
    }

    private fun greeting(): UiText {
        val who = profileName ?: email ?: return R.string.home_greeting.toUiText()
        return R.string.home_greeting_named.toUiText(who)
    }

    private fun setDoor(id: String, change: HomeDoorState.() -> HomeDoorState) = updateData {
        copy(doors = doors.map { if (it.id == id) it.change() else it })
    }
}

/** The doors placed on Home, by section and then their own order. */
private fun List<ShellDoor>.onHome(): List<ShellDoor> =
    filter { ShellPlacement.Home in it.placements }.sortedWith(compareBy({ it.section.ordinal }, { it.order }))
