package com.example.androidproject1.feature.auth.domain

/**
 * The terms this build asks for, and where to read them. Built in `:app` from `BuildConfig`
 * — `ProjectConfig.Legal` is where the numbers live — and handed in, since a feature cannot read
 * the application module's build config.
 *
 * [version] goes up when the terms change; a session that accepted a lower one is asked again.
 */
data class TermsPolicy(
    val version: Int,
    val termsUrl: String,
    val privacyUrl: String,
) {

    companion object {

        /** What a session stored before terms were versioned is taken to have accepted. */
        const val FIRST_VERSION = 1
    }
}
