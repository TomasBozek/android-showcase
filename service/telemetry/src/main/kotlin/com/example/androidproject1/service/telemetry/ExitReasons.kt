package com.example.androidproject1.service.telemetry

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.time.Instant

/** How an earlier process of this app ended, as the system recorded it. */
public data class ExitReason(
    val at: Instant,
    val kind: CrashKind,
    val description: String,
    val trace: String?,
)

/** The system's record of how this app's earlier processes ended. */
public fun interface ExitReasons {

    /** The crashes and ANRs that ended a process after [after], newest first. */
    public fun since(after: Instant): List<ExitReason>
}

/**
 * [ExitReasons] from `ActivityManager.getHistoricalProcessExitReasons`, API 30 and up; below that
 * the system keeps none and this returns nothing. A Java crash is left out: the uncaught-exception
 * handler wrote it with its real stack, which the system's record of it does not carry.
 */
public class AndroidExitReasons(private val context: Context) : ExitReasons {

    override fun since(after: Instant): List<ExitReason> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val manager = context.getSystemService(ActivityManager::class.java) ?: return emptyList()
        return manager.getHistoricalProcessExitReasons(context.packageName, 0, MAX_REASONS)
            .filter { Instant.ofEpochMilli(it.timestamp) > after }
            .mapNotNull { info ->
                val kind = when (info.reason) {
                    ApplicationExitInfo.REASON_ANR -> CrashKind.Anr
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> CrashKind.NativeCrash
                    else -> return@mapNotNull null
                }
                ExitReason(
                    at = Instant.ofEpochMilli(info.timestamp),
                    kind = kind,
                    description = info.description ?: kind.name,
                    trace = runCatching {
                        info.traceInputStream?.bufferedReader()?.use { it.readCapped() }
                    }.getOrNull(),
                )
            }
    }

    private companion object {

        const val MAX_REASONS = 10
    }
}

// A system dump can run to megabytes; nothing past what a record keeps is read into memory.
private fun java.io.Reader.readCapped(): String {
    val buffer = CharArray(FileCrashLog.MAX_TRACE_CHARS)
    var filled = 0
    while (filled < buffer.size) {
        val read = read(buffer, filled, buffer.size - filled)
        if (read < 0) break
        filled += read
    }
    return String(buffer, 0, filled)
}
