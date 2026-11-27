package com.example.androidproject1.service.network

import java.util.UUID

/**
 * What every request says about where it came from, so a support ticket can find it in a
 * server log: `User-Agent` names the app, its version and the OS, `Accept-Language` the language
 * the app is in now, and `X-Request-Id` the request itself.
 *
 * Plain values rather than `DeviceInfo`: this module is Kotlin/JVM and the device facts are an
 * Android module's — `:core:di` copies them across.
 *
 * @param language asked on every request, because the per-app language can change while the app runs.
 * @param newRequestId one per request; a retry of it keeps it, so the server sees one request tried twice.
 */
public class RequestIdentity(
    applicationId: String,
    versionName: String,
    versionCode: Int,
    osVersion: String,
    model: String,
    public val language: () -> String,
    public val newRequestId: () -> String = { UUID.randomUUID().toString() },
) {

    /** `com.example.app/1.8.0 (412; Android 15; Google Pixel 9)`. */
    public val userAgent: String = "$applicationId/$versionName ($versionCode; $osVersion; $model)"

    public companion object {
        public const val REQUEST_ID_HEADER: String = "X-Request-Id"
    }
}
