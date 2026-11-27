package com.example.androidproject1.service.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.core.os.CancellationSignal
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [LocationProvider] over `LocationManagerCompat`.
 *
 * The provider is the best *enabled* one this app may read: the fused one on API 31+, otherwise
 * the network's. GPS is not used — it needs the precise permission this module does not declare,
 * and a city does not need it. Every platform call is guarded by [hasPermission] and, where the
 * platform can still refuse, by a catch: a permission revoked between the check and the call is a
 * failed fix, not a crash.
 */
public class PlatformLocationProvider(
    private val context: Context,
    private val timeout: Duration = 10.seconds,
) : LocationProvider {

    private val manager: LocationManager? = context.getSystemService()

    /** Where a callback runs: right where the platform calls it, which is cheap here. */
    private val direct = Executor { it.run() }

    override suspend fun current(): Outcome<Fix> {
        if (!hasPermission()) return Outcome.Failure(LocationPermissionError())
        val manager = manager ?: return unavailable("no location service")
        val provider = enabledProvider(manager) ?: return unavailable("no location provider is enabled")

        val fix = withTimeoutOrNull(timeout) { currentFrom(manager, provider) }
            // A device that took too long may still know a recent answer.
            ?: lastKnown()
        return fix?.let { Outcome.Success(it) } ?: unavailable("no fix within $timeout")
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentFrom(manager: LocationManager, provider: String): Fix? =
        suspendCancellableCoroutine { continuation ->
            val cancel = CancellationSignal()
            continuation.invokeOnCancellation { cancel.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(manager, provider, cancel, direct) { location ->
                    if (continuation.isActive) continuation.resume(location?.toFix())
                }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    override fun updates(): Flow<Fix> = callbackFlow {
        val manager = manager
        val provider = manager?.let(::enabledProvider)
        if (manager == null || provider == null || !hasPermission()) {
            close()
            return@callbackFlow
        }
        val listener = LocationListenerCompat { location -> trySend(location.toFix()) }
        try {
            LocationManagerCompat.requestLocationUpdates(
                manager,
                provider,
                LocationRequestCompat.Builder(UPDATE_INTERVAL_MS).build(),
                direct,
                listener,
            )
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        }
        awaitClose { manager.removeUpdates(listener) }
    }

    @SuppressLint("MissingPermission")
    override suspend fun lastKnown(): Fix? {
        val manager = manager ?: return null
        if (!hasPermission()) return null
        return try {
            candidateProviders().mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }?.toFix()
        } catch (_: SecurityException) {
            null
        }
    }

    private fun enabledProvider(manager: LocationManager): String? =
        candidateProviders().firstOrNull { manager.isProviderEnabled(it) }

    private fun candidateProviders(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }

    private fun hasPermission(): Boolean = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    private fun unavailable(reason: String): Outcome<Fix> = Outcome.Failure(LocationUnavailableError(reason))

    private fun Location.toFix() = Fix(latitude = latitude, longitude = longitude, accuracyM = accuracy)

    private companion object {

        const val UPDATE_INTERVAL_MS = 10_000L
    }
}
