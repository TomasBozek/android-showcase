package com.example.androidproject1.service.work

import kotlin.time.Duration

/**
 * Work that runs while the app is closed. One port for one-shot, delayed, periodic, constrained
 * and expedited work, so a caller says *what* and *when* and never names WorkManager.
 *
 * Everything is **named**: scheduling a name again replaces the work it named (or keeps it, when
 * [WorkSpec.replace] is false), which is what makes "the reminder for trip 7" one thing however
 * many times the trip is saved.
 */
public interface Scheduler {

    public fun schedule(spec: WorkSpec)

    /** Whatever [name] named, if it has not run: a delayed or periodic job never will. */
    public fun cancel(name: String)
}

/**
 * One piece of work: the [handler] whose key does it, and the [input] it is handed.
 *
 * [input] is strings only and small (WorkManager caps it at 10 KB): an id to look the rest up by,
 * not the thing itself. A [repeat] of less than 15 minutes is raised to 15 by the platform.
 * [expedited] work starts at once but may be run as ordinary work when the app is out of quota;
 * below API 31 its handler must supply a [WorkHandler.foregroundInfo].
 */
public data class WorkSpec(
    val name: String,
    val handler: String,
    val input: Map<String, String> = emptyMap(),
    val delay: Duration = Duration.ZERO,
    val repeat: Duration? = null,
    val constraints: WorkConstraints = WorkConstraints(),
    val expedited: Boolean = false,
    val replace: Boolean = true,
)

public data class WorkConstraints(
    val network: NetworkRequirement = NetworkRequirement.None,
    val charging: Boolean = false,
    val batteryNotLow: Boolean = false,
)

public enum class NetworkRequirement { None, Connected, Unmetered }
