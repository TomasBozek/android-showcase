package com.example.androidproject1.feature.profile.api

import com.example.androidproject1.feature.settings.api.SettingsDestination
import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.bare

/** `profile`: the profile, over the Settings tab it is reached from. */
val profileDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "profile" to bare(SettingsDestination, ProfileDestination),
)
