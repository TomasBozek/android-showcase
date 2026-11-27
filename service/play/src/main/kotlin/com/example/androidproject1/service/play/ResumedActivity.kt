package com.example.androidproject1.service.play

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * The activity in front, held weakly. Play draws its review sheet and its update flow over an
 * `Activity`, and whatever asks is a singleton with none — holding one there would leak it.
 *
 * Made with the graph (`createdAtStart`) so it has seen the first activity resume by the time
 * anything asks.
 */
internal class ResumedActivity(application: Application) {

    @Volatile
    private var resumed = WeakReference<Activity>(null)

    val activity: Activity? get() = resumed.get()

    init {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    resumed = WeakReference(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    if (resumed.get() === activity) resumed = WeakReference(null)
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }
}
