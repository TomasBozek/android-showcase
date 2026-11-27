package com.example.androidproject1.wiring.checkout

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * What only `:app` can give the checkout: the flavor's [checkoutPrefill], an example address
 * and card on `dev` and nothing elsewhere, from the flavor's own source set. `ApplicationModule`
 * includes it in one line, and `delete_feature.py checkout` deletes this package in every source
 * set and that line with it.
 */
val checkoutWiring: Module = module {
    single { checkoutPrefill() }
}
