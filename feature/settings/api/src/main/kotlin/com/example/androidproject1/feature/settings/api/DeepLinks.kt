package com.example.androidproject1.feature.settings.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.bare

/** `settings`: the Settings tab. */
val settingsDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "settings" to bare(SettingsDestination),
)
