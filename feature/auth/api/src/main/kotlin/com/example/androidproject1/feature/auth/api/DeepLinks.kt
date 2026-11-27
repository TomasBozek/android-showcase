package com.example.androidproject1.feature.auth.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import com.example.androidproject1.service.deeplink.withId

/**
 * `reset/<token>`, from the password-reset mail. The one link that needs no session: it is
 * followed by someone who cannot sign in, so it lands on whichever flow is up.
 */
val authDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "reset" to withId({ it }) { DeepLinkTarget(listOf(ResetPasswordDestination(token = it)), needsSession = false) },
)
