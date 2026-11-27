package com.example.androidproject1.service.telemetry

import java.io.File
import java.time.Instant

/** What ended the process. */
public enum class CrashKind { Crash, Anr, NativeCrash }

/**
 * One crash or ANR as the phone kept it.
 *
 * @property summary one line: the exception and its message, or the system's description of an ANR.
 * @property trace the stack, or the system's thread dump for an ANR or a native crash, capped.
 */
public data class CrashRecord(
    val at: Instant,
    val kind: CrashKind,
    val summary: String,
    val trace: String,
)

/**
 * The crashes and ANRs this install has had, kept on the phone and nowhere else — the last
 * few, for a tester to share from the debug menu and for the launch after one to offer to send.
 * Nothing leaves the device unless a person shares it. A vendor's crash reporter is a different
 * thing and binds behind [ErrorTracker].
 */
public interface CrashLog {

    /** Adds the ANRs and native crashes the system recorded since the last call; once per launch. */
    public suspend fun collect()

    /** The records kept, newest first. */
    public suspend fun recent(): List<CrashRecord>

    /** The newest record no launch has been told about yet, marked as told; `null` when there is none. */
    public suspend fun takeNew(): CrashRecord?

    /** Every record, newest first, as plain text in [into]. */
    public suspend fun writeReport(into: File): File
}
