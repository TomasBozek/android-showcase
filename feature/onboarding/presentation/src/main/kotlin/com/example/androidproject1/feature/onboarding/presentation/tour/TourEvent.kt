package com.example.androidproject1.feature.onboarding.presentation.tour

/** What the tour's overlay reports. Not a `UiEvent`: the tour is not a screen. */
sealed interface TourEvent {

    /** The next step, or the end of the tour — which marks it seen. */
    data object Next : TourEvent

    /** Left on purpose, which also marks it seen: a tour offered twice is a tour nobody trusts. */
    data object Skip : TourEvent
}
