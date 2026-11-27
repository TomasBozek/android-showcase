package com.example.androidproject1.feature.devmenu.presentation

/**
 * Forgets which version's What's new was seen, so the next start shows it.
 *
 * A seam for the same reason [SessionReadout] is one: `:app` binds it in `wiring.onboarding`, and an
 * app without the sheet binds nothing and the button is not drawn.
 */
interface WhatsNewReset {

    suspend fun reset()
}
