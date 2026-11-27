package com.example.androidproject1.service.play

import android.app.Application
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** How an update is taken. */
public enum class UpdateKind {

    /** A full-screen flow the user cannot leave until it is installed. For a build that must not run any longer. */
    Immediate,

    /** Downloaded in the background while the app is used; the app asks to install when it is ready. */
    Flexible,
}

/** A newer build Play has for this device. */
public data class UpdateOffer(
    val versionCode: Int,
    /** How long Play has had it and this device not, in days; `null` if it has never said. */
    val staleDays: Int?,
    /** Play's own 0–5 priority for the update, as set in the developer console. */
    val priority: Int,
    val immediateAllowed: Boolean,
    val flexibleAllowed: Boolean,
)

/** How starting an update went. */
public enum class UpdateStart {

    /** The flow is on screen (or downloading). */
    Started,

    /** The user turned it down. */
    Declined,

    /** Nothing in front to show it over, no update, or this kind is not allowed for it. */
    NotStarted,
}

/**
 * In-app update: is there a newer build, and take it without leaving the app.
 *
 * Behind a port because it is Play's, and `dev` — with no store account — runs Google's fake
 * manager instead (`PlayConfig`). What decides *when* to offer one is the caller's.
 */
public interface AppUpdates {

    /** The offer Play has for this device now, or `null`: no update, no route, or the app is not from Play. */
    public suspend fun check(): UpdateOffer?

    /** Starts [kind] for the offer [check] returned. */
    public suspend fun start(kind: UpdateKind): UpdateStart

    /** Installs a flexible update that has finished downloading; the app restarts. */
    public fun completeFlexible()

    /**
     * Emits when a flexible update has finished downloading and is waiting to be installed — also
     * for one that finished in an earlier session, the moment this is collected.
     */
    public val downloaded: Flow<Unit>
}

/** [AppUpdates] over `AppUpdateManager`, showing its flow over the activity in front. */
public class PlayAppUpdates(
    application: Application,
    private val manager: AppUpdateManager,
) : AppUpdates {

    private val front = ResumedActivity(application)
    private var info: AppUpdateInfo? = null

    override suspend fun check(): UpdateOffer? {
        val found = runCatching { manager.appUpdateInfo.await() }.getOrNull()
        info = found
        if (found == null || found.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return null
        return UpdateOffer(
            versionCode = found.availableVersionCode(),
            staleDays = found.clientVersionStalenessDays(),
            priority = found.updatePriority(),
            immediateAllowed = found.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            flexibleAllowed = found.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
        )
    }

    override suspend fun start(kind: UpdateKind): UpdateStart {
        val offer = info ?: return UpdateStart.NotStarted
        val activity = front.activity ?: return UpdateStart.NotStarted
        val type = if (kind == UpdateKind.Immediate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
        if (!offer.isUpdateTypeAllowed(type)) return UpdateStart.NotStarted
        val result = runCatching {
            manager.startUpdateFlow(offer, activity, AppUpdateOptions.defaultOptions(type)).await()
        }.getOrNull()
        return when (result) {
            android.app.Activity.RESULT_OK -> UpdateStart.Started
            android.app.Activity.RESULT_CANCELED -> UpdateStart.Declined
            else -> UpdateStart.NotStarted
        }
    }

    override fun completeFlexible() {
        manager.completeUpdate()
    }

    override val downloaded: Flow<Unit> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) trySend(Unit)
        }
        manager.registerListener(listener)
        // One that finished while the app was closed is still waiting to be installed.
        val waiting = runCatching { manager.appUpdateInfo.await() }.getOrNull()
        if (waiting?.installStatus() == InstallStatus.DOWNLOADED) trySend(Unit)
        awaitClose { manager.unregisterListener(listener) }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWith(Result.failure(it)) }
    }
}
