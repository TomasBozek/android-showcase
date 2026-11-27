package com.example.androidproject1.service.permission

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * A permission's status, for a view model to read without a `Context`.
 *
 * The screen reads which permissions the manifest declares — `rememberDeclaredPermissions()` — and
 * hands them over; the view model asks this what each one's status is, so rationale and
 * refused-for-good are states it decides on rather than branches a composable hides.
 */
public interface PermissionChecker {

    public fun status(permission: DeclaredPermission): PermissionStatus
}

/**
 * [PermissionChecker] over the platform: held or not from [DeclaredPermission.isGranted], and the
 * rest from whether this app has asked before — recorded by `rememberPermissionRequest` — and from
 * the resumed activity's `shouldShowRequestPermissionRationale`, which only an activity can answer.
 */
public class AndroidPermissionChecker(private val application: Application) : PermissionChecker {

    @Volatile
    private var resumed: WeakReference<Activity>? = null

    init {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    resumed = WeakReference(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    if (resumed?.get() === activity) resumed = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    override fun status(permission: DeclaredPermission): PermissionStatus = statusOf(
        granted = permission.isGranted,
        requested = application.hasRequested(permission.name),
        canAskAgain = resumed?.get()?.shouldShowRequestPermissionRationale(permission.name) == true,
    )
}

/** The one rule both the checker and the request apply, so the two can never disagree. */
internal fun statusOf(granted: Boolean, requested: Boolean, canAskAgain: Boolean): PermissionStatus = when {
    granted -> PermissionStatus.Granted
    !requested -> PermissionStatus.NotRequested
    canAskAgain -> PermissionStatus.NeedsRationale
    else -> PermissionStatus.RefusedForGood
}

// `shouldShowRequestPermissionRationale` is false both before the first ask and after a refusal
// for good, so "have we asked?" is remembered here — on disk, or a refusal for good reads as not
// asked once the process has died.
private const val REQUESTED = "service_permission_requested"

internal fun Context.hasRequested(permission: String): Boolean =
    getSharedPreferences(REQUESTED, Context.MODE_PRIVATE).getBoolean(permission, false)

internal fun Context.markRequested(permissions: List<String>) {
    val editor = getSharedPreferences(REQUESTED, Context.MODE_PRIVATE).edit()
    permissions.forEach { editor.putBoolean(it, true) }
    editor.apply()
}
