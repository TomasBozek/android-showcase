package com.example.androidproject1.service.work

import androidx.work.ForegroundInfo

/**
 * What runs when a [WorkSpec] naming [key] is due.
 *
 * A feature binds one in its Koin module and the app's `WorkerFactory` finds it by key, so a
 * handler is an ordinary class with ordinary constructor injection — not a `Worker` subclass that
 * WorkManager builds by reflection.
 */
public interface WorkHandler {

    public val key: String

    public suspend fun run(input: Map<String, String>): WorkResult

    /**
     * The notification an expedited job shows below API 31, where it runs as a foreground service.
     * `null` for a handler whose work is never expedited.
     */
    public suspend fun foregroundInfo(input: Map<String, String>): ForegroundInfo? = null
}

public enum class WorkResult {
    Success,

    /** Not done: WorkManager tries again, later, with its backoff. */
    Retry,
    Failure,
}
