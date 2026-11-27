package com.example.androidproject1.feature.checkout.domain

import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The wizard's draft between its steps. Each step reads what the steps before it filled and writes
 * its own part; the route arguments stay empty, so going back a step finds what was typed there.
 *
 * One per app, from the feature's Koin module, and as long as one checkout: leaving the wizard from
 * its first step or placing the order [clear]s it. It outlives the app: every change is
 * written to [repository], and a step [restore]s before it reads, so a checkout left on any step
 * resumes there after a process death or a relaunch. The card and the payment token stay in
 * memory only — a resumed checkout asks for the payment again.
 *
 * A checkout starts from [prefill] rather than from nothing, and [clear] goes back to it: empty in
 * a shipped build, the `dev` flavor's example address and card there. A saved address wins over
 * the prefilled one.
 *
 * Account data, so a sign-out [erase]s it.
 */
class CheckoutDraftStore(
    prefill: CheckoutPrefill,
    private val repository: CheckoutDraftRepository,
) : LocalDataEraser {

    private val blank = CheckoutDraft(address = prefill.address, card = prefill.card)

    private val state = MutableStateFlow(blank)

    val draft: StateFlow<CheckoutDraft> = state.asStateFlow()

    private val readLock = Mutex()
    private var restored = false

    // Writes are serialised and each one writes the draft as it is when its turn comes, so the
    // last write always holds the newest draft.
    private val writeLock = Mutex()

    /**
     * The saved draft merged into this one, read once per process; the draft after. A failed read
     * is a checkout that starts afresh, not an error — there is nothing the user could do about it.
     */
    suspend fun restore(): CheckoutDraft = readLock.withLock {
        if (!restored) {
            val saved = (repository.read() as? Outcome.Success)?.data
            if (saved != null) {
                state.update { current ->
                    current.copy(
                        address = saved.address ?: current.address,
                        delivery = saved.delivery ?: current.delivery,
                        promo = saved.promo ?: current.promo,
                    )
                }
            }
            restored = true
        }
        state.value
    }

    /** Changes the draft and writes it, after reading the saved one so an early edit cannot wipe it. */
    suspend fun update(transform: (CheckoutDraft) -> CheckoutDraft) {
        restore()
        state.update(transform)
        persist { repository.save(state.value) }
    }

    suspend fun clear() {
        readLock.withLock { restored = true }
        state.value = blank
        persist { repository.clear() }
    }

    override suspend fun erase() {
        clear()
    }

    // Not cancellable: a step that clears and leaves cancels its scope as it goes, and a write cut
    // off there would bring the old draft back on the next launch.
    private suspend fun persist(write: suspend () -> Outcome<Unit>) {
        withContext(NonCancellable) { writeLock.withLock { write() } }
    }
}
