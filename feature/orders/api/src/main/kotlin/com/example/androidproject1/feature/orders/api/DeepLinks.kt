package com.example.androidproject1.feature.orders.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import com.example.androidproject1.service.deeplink.bare
import com.example.androidproject1.service.deeplink.withId

/**
 * `orders` and `order/<id>`: the list, and one order above it — what a confirmation e-mail
 * or a delivery notification links to. The list is reached from Profile, whose keys are not in an
 * api module yet, so `:app` puts Settings and Profile under these on a cold start.
 */
val ordersDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "orders" to bare(OrdersDestination),
    "order" to withId({ it }) { DeepLinkTarget(listOf(OrdersDestination, OrderDetailDestination(orderId = it))) },
)
