package com.example.androidproject1.service.network

/**
 * The one place a `401` no refresh could answer ends the session.
 *
 * A port in this module, because [HttpClientFactory] is the caller and cannot see the module that
 * owns the session; `:service:session` implements it. Called at most once per failed request, so a
 * burst of them is a burst of calls the implementation collapses.
 */
public interface SessionEnder {

    public suspend fun end(reason: EndReason)
}

/** Why the session ended. One reason today; a `403` is never one. */
public enum class EndReason {

    /** The server refused the tokens, and refreshing them did not help. */
    Unauthorized,
}
