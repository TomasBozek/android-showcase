package com.example.androidproject1.service.session

import com.example.androidproject1.service.network.Tokens

/**
 * Who is signed in, and what proves it.
 *
 * [tokens] is `null` for a session written before this module kept tokens, and for one an app
 * that has no token API signs in with: such a session is still a session, and simply sends no
 * bearer.
 *
 * [termsVersion] is the terms version the account last accepted — the session keeps the number and
 * the app decides what it means.
 */
public data class Session(
    val id: String,
    val email: String,
    val termsVersion: Int,
    val tokens: Tokens?,
)
