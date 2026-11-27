package com.example.androidproject1.service.session

import kotlinx.coroutines.flow.Flow

/**
 * The one place a session is kept. Nothing else stores who is signed in or holds a token.
 *
 * A [Session] is written whole, so a sign-in, a terms acceptance and a refreshed token pair are
 * all one write of one record and none of them can leave the others behind.
 */
public interface SessionStore {

    /** `null` when nobody is signed in — and for anything stored that this build cannot read. */
    public val state: Flow<Session?>

    public suspend fun save(session: Session)

    public suspend fun clear()
}
