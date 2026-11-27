package com.example.androidproject1.feature.catalog.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ProductsDestination(val categoryId: String, val categoryName: String? = null) : NavKey
