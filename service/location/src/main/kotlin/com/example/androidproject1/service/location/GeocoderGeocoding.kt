package com.example.androidproject1.service.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

/**
 * [Geocoding] over Android's `Geocoder`, in the user's language. Open-Meteo cannot do this
 * direction, and nothing else is asked: no key, no second service.
 */
public class GeocoderGeocoding(
    context: Context,
    /** Made per ask, so the language is the one the user has now. A parameter so a test can hand it one it controls. */
    private val newGeocoder: () -> Geocoder = { Geocoder(context, Locale.getDefault()) },
) : Geocoding {

    override val isAvailable: Boolean get() = Geocoder.isPresent()

    override suspend fun name(fix: Fix): String? {
        if (!isAvailable) return null
        val geocoder = newGeocoder()
        val address = try {
            fromLocation(geocoder, fix)
        } catch (_: IOException) {
            // The backing service is there but could not be reached: the same answer as no answer.
            null
        }
        return address?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }

    @Suppress("DEPRECATION")
    private suspend fun fromLocation(geocoder: Geocoder, fix: Fix): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    fix.latitude,
                    fix.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                )
            }
        } else {
            // The blocking overload: it talks to a service, so not on the caller's thread.
            withContext(Dispatchers.IO) { geocoder.getFromLocation(fix.latitude, fix.longitude, 1)?.firstOrNull() }
        }
}
