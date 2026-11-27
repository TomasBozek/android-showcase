package com.example.androidproject1.feature.onboarding.presentation.tour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.onboarding.domain.OnboardingRepository
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Where the tour is, and the one flag that says whether it should run at all.
 *
 * A plain [ViewModel] rather than a `BaseViewModel`, for the reason `MainViewModel` is one: it is
 * not a screen. There is no loading overlay to raise and no error dialog to show — and going
 * through `Screen()` would paint a full-size surface over the screen the tour is pointing at,
 * which is the whole thing it must not do.
 *
 * The host says [start]; this decides. A tour already seen does not run, and one already running
 * is not restarted — Home recomposes and the device rotates, and a tour that jumped back to its
 * first step each time would never be walkable.
 */
class TourViewModel(
    logger: Logger,
    private val onboardingRepository: OnboardingRepository,
) : ViewModel() {

    private val logger = logger.withTag(TAG)

    private val _state = MutableStateFlow(TourState())
    val state: StateFlow<TourState> = _state.asStateFlow()

    /** The steps the host gave, kept so a replay has something to replay. */
    private var steps: List<TourStep> = emptyList()

    private var watching: Job? = null

    private var paused = false

    /**
     * Offers the tour, and keeps offering it: the steps are remembered and the flag watched, so a
     * reset from anywhere — the Settings row, the drawer row — starts the tour again without the
     * host being told.
     *
     * A tour already running is not restarted. Home recomposes and the device rotates, and one
     * that jumped back to its first step each time would never be walkable.
     */
    fun start(steps: List<TourStep>) {
        if (steps.isEmpty() || steps == this.steps) return
        this.steps = steps
        if (!paused) watch()
    }

    private fun watch() {
        watching?.cancel()
        watching = viewModelScope.launch {
            onboardingRepository.observeTourSeen().collect { outcome ->
                when (outcome) {
                    is Outcome.Failure -> logger.w { "the tour flag could not be read: ${outcome.error}" }
                    is Outcome.Success -> if (!outcome.data && !_state.value.running) {
                        _state.value = TourState(step = 0, steps = steps)
                    }
                }
            }
        }
    }

    /**
     * Steps aside for the rest of this launch without marking the tour seen: a link opened over it
     * is somewhere the user asked to go, and a scrim over that screen is in the way. Offered
     * again on the next launch, since it was never walked.
     */
    fun pause() {
        paused = true
        watching?.cancel()
        _state.value = TourState()
    }

    /**
     * Puts the flag back, so the tour runs again. What the drawer's row does; the Settings row
     * reaches the same flag through its own view model, because a screen talks to a repository
     * rather than to another screen's view model.
     */
    fun replay() {
        // Asked for by name, so a pause for a link is over.
        if (paused) {
            paused = false
            if (steps.isNotEmpty()) watch()
        }
        viewModelScope.launch {
            val outcome = onboardingRepository.resetTour()
            if (outcome is Outcome.Failure) logger.w { "the tour could not be reset: ${outcome.error}" }
        }
    }

    fun onEvent(event: TourEvent) {
        when (event) {
            TourEvent.Next -> next()
            TourEvent.Skip -> finish()
        }
    }

    private fun next() {
        val state = _state.value
        val step = state.step ?: return
        if (step + 1 < state.steps.size) _state.value = state.copy(step = step + 1) else finish()
    }

    /**
     * Ends the tour and writes the flag, whether it was walked or left.
     *
     * Both, because a tour offered a second time to someone who skipped it is a tour they learn
     * to dismiss. Coming back is the Settings row's job, which resets the flag on purpose.
     *
     * The flag is written **before** the state is cleared. The other way round, the watcher above
     * would see a tour that is no longer running while the stored flag still said `false`, and
     * start the whole thing again on the step the user just finished.
     */
    private fun finish() {
        viewModelScope.launch {
            val outcome = onboardingRepository.markTourSeen()
            if (outcome is Outcome.Failure) logger.w { "the tour flag could not be written: ${outcome.error}" }
            _state.value = TourState()
        }
    }

    private companion object {

        const val TAG = "TourViewModel"
    }
}
