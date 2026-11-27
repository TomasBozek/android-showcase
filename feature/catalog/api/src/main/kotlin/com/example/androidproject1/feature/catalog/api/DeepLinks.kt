package com.example.androidproject1.feature.catalog.api

import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import com.example.androidproject1.service.deeplink.bare
import com.example.androidproject1.service.deeplink.withId

/**
 * `catalog`, `product/<id>`, `category/<id>` and `search`, each under the Catalog tab's
 * root so Up walks back through it after a cold start — the shell puts Home under it and looks
 * nothing up. A category link carries no name: the products screen looks it up.
 */
val catalogDeepLinks: Map<String, DeepLinkTable> = mapOf(
    "catalog" to bare(CategoriesDestination),
    "product" to
        withId({ it }) { DeepLinkTarget(listOf(CategoriesDestination, ProductDetailDestination(productId = it))) },
    "category" to withId({
        it
    }) { DeepLinkTarget(listOf(CategoriesDestination, ProductsDestination(categoryId = it))) },
    "search" to bare(CategoriesDestination, ProductSearchDestination),
)
