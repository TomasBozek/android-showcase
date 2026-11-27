package com.example.androidproject1.feature.auth.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The wait before another mail may be asked for: a reset link or a sign-up code.
 *
 * One tick a second in the view model's [scope], so a rotation carries on counting; [onTick]
 * reports the seconds left, reaching `0` when resend opens. Shared rather than copied, because two
 * screens counting the same thirty seconds two ways is how one of them ends up at twenty-nine.
 */
class ResendCountdown(
    private val scope: CoroutineScope,
    private val onTick: (secondsLeft: Int) -> Unit,
) {

    private var job: Job? = null

    /** Starts over from [from] seconds; `0` reports once and opens resend at once. */
    fun start(from: Int = SECONDS) {
        job?.cancel()
        onTick(from)
        job = scope.launch {
            for (left in from - 1 downTo 0) {
                delay(TICK_MS)
                onTick(left)
            }
        }
    }

    companion object {

        /** How long before another mail. The `dev` fixture refuses a reset inside the same window. */
        const val SECONDS = 30

        private const val TICK_MS = 1_000L
    }
}
