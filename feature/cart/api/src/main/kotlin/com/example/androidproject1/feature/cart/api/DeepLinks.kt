package com.example.androidproject1.feature.cart.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.bare

/** `cart`: the Cart tab. */
val cartDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "cart" to bare(CartDestination),
)
