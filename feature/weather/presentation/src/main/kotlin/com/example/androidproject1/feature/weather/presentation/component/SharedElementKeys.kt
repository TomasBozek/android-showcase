package com.example.androidproject1.feature.weather.presentation.component

/**
 * The key a saved place's row and its forecast share: the place's name, which is the row's
 * headline and the forecast's title, so the name a finger landed on is the name that ends up in
 * the top bar rather than one fading out as another fades in.
 *
 * One string per place, so no two rows name the same thing. The catalog's own pattern.
 */
fun placeNameKey(id: Int): String = "place/$id/name"
