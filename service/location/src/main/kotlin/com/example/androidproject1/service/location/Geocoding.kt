package com.example.androidproject1.service.location

/**
 * A place's name from where it is. A port, because the platform's `Geocoder` is backed by a
 * service some devices do not have.
 */
public interface Geocoding {

    /** Whether this device can name a place at all. When it cannot, save the place as its coordinates. */
    public val isAvailable: Boolean

    /** The place's name — its locality — or `null` when there is no geocoder or it knows nothing there. */
    public suspend fun name(fix: Fix): String?
}
