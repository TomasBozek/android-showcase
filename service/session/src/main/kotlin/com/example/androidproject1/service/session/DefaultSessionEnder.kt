package com.example.androidproject1.service.session

import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.network.EndReason
import com.example.androidproject1.service.network.SessionEnder

/**
 * The one place a session is ended from outside a screen.
 *
 * It says so and does nothing else: signing out is the owner's, so this hands the owner the
 * news through [SessionExpiry] and the owner clears the store. Clearing it here would flip the
 * session to signed out before the owner had looked, and the owner ignores news that arrives when
 * nobody is signed in.
 */
public class DefaultSessionEnder(
    private val expiry: SessionExpiry,
    logger: Logger,
) : SessionEnder {

    private val logger = logger.withTag("DefaultSessionEnder")

    override suspend fun end(reason: EndReason) {
        logger.w { "Ending the session: $reason" }
        expiry.signal()
    }
}
