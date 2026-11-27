package com.example.androidproject1.feature.home.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.bare

/** `home`: the Home tab. */
val homeDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "home" to bare(HomeDestination),
)
