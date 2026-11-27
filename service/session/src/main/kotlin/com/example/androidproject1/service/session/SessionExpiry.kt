package com.example.androidproject1.service.session

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The server said the session is over.
 *
 * Two parties and one object between them: [DefaultSessionEnder] [signal]s when the HTTP client
 * reports a `401` no refresh could answer, and the owner of the session collects [signals], signs
 * out and says why through the app's message outlet. Neither knows the other — the client
 * does not know there is a session.
 *
 * Bound once, as a singleton: a second instance is a signal nobody hears.
 */
public interface SessionExpiry {

    /** One per ended session. Collected by one owner, for the life of the process. */
    public val signals: Flow<Unit>

    /** Cheap and never blocks: a burst of them is one signal. */
    public fun signal()
}

public class DefaultSessionExpiry : SessionExpiry {

    // One slot and drop-oldest: ten requests failing together are one reason to sign out, not ten.
    private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val signals: Flow<Unit> = flow.asSharedFlow()

    override fun signal() {
        flow.tryEmit(Unit)
    }
}
