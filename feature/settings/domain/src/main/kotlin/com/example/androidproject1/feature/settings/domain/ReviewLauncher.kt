package com.example.androidproject1.feature.settings.domain

/**
 * Shows the store's own rating sheet. Implemented in `:app`, which is the only module
 * that has Play's review library — and binds a fake on `dev`, so the flow completes on an
 * emulator with no store account.
 *
 * Play decides whether the sheet actually appears and never says whether the user rated, so this
 * returns when the flow is over, not what happened in it.
 */
interface ReviewLauncher {

    suspend fun launch()
}
