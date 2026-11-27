package com.example.androidproject1.feature.settings.domain

/**
 * Which build this is, and where its legal pages live — as `:app` knows it.
 *
 * Declared here and built in `:app` from `BuildConfig`: a feature that read
 * `BuildConfig` itself would depend on the application module. The version, the OS and the model
 * are `:service:device`'s `DeviceInfo`, so the About screen, the dev menu and a feedback mail read
 * one place.
 */
data class AppInfo(
    /** `dev`, `staging` or `prod`. */
    val flavor: String,
    /** `debug` or `release`. */
    val buildType: String,
    val privacyUrl: String,
    val termsUrl: String,
    /** Where Settings → Send feedback writes to. */
    val supportEmail: String,
    /** What Settings → Help opens. */
    val helpUrl: String,
)
