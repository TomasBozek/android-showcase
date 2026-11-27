package com.example.androidproject1.service.telemetry

import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Clock
import java.time.Instant

/**
 * [CrashLog] as one small text file per record in [directory], the newest [MAX_RECORDS] kept.
 *
 * A crash is written by [install]'s uncaught-exception handler, **synchronously**, on the thread
 * that is about to die — there is no later. An ANR or a native crash cannot be caught in process,
 * so the next launch [collect]s what the system recorded, its trace capped at [MAX_TRACE_CHARS].
 * Two marker files say which system record was collected last and which record a launch was last
 * told about, so neither comes round twice.
 *
 * A record is the install's, not the account's: a sign-out keeps it, an account deletion takes it.
 */
public class FileCrashLog(
    private val directory: () -> File,
    private val exitReasons: ExitReasons,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
) : CrashLog,
    LocalDataEraser {

    override val erasesOnSignOut: Boolean = false

    /** Chains in front of whatever handler was installed, which still runs — the system's dialog, a vendor's. */
    public fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Writes [throwable] as a crash, now and on this thread. */
    public fun record(thread: Thread, throwable: Throwable) {
        write(
            CrashRecord(
                at = Instant.now(clock),
                kind = CrashKind.Crash,
                summary = throwable.toString(),
                trace = "Thread: ${thread.name}\n" + throwable.stackTraceToString().take(MAX_TRACE_CHARS),
            ),
        )
    }

    override suspend fun collect(): Unit = withContext(dispatchers.io) {
        val marker = marker(COLLECTED)
        val after = marker.readInstant() ?: Instant.EPOCH
        val reasons = exitReasons.since(after)
        reasons.forEach { reason ->
            write(
                CrashRecord(
                    at = reason.at,
                    kind = reason.kind,
                    summary = reason.description,
                    trace = reason.trace.orEmpty().take(MAX_TRACE_CHARS),
                ),
            )
        }
        reasons.maxOfOrNull { it.at }?.let { marker.writeText(it.toEpochMilli().toString()) }
    }

    override suspend fun recent(): List<CrashRecord> = withContext(dispatchers.io) {
        recordFiles().mapNotNull(::read)
    }

    override suspend fun takeNew(): CrashRecord? = withContext(dispatchers.io) {
        val newest = recordFiles().firstOrNull()?.let(::read) ?: return@withContext null
        val marker = marker(TOLD)
        val told = marker.readInstant()
        if (told != null && !newest.at.isAfter(told)) return@withContext null
        marker.writeText(newest.at.toEpochMilli().toString())
        newest
    }

    override suspend fun writeReport(into: File): File = withContext(dispatchers.io) {
        into.writeText(recordFiles().joinToString(separator = "\n\n") { it.readText() })
        into
    }

    override suspend fun erase(): Unit = withContext(dispatchers.io) {
        directory().listFiles()?.forEach(File::delete)
        Unit
    }

    private fun write(record: CrashRecord) {
        val dir = directory().apply { mkdirs() }
        var file = File(dir, "$PREFIX${record.at.toEpochMilli()}.txt")
        var suffix = 1
        while (file.exists()) file = File(dir, "$PREFIX${record.at.toEpochMilli()}-${suffix++}.txt")
        file.writeText(
            buildString {
                append("kind: ").append(record.kind.name).append('\n')
                append("at: ").append(record.at).append('\n')
                append("summary: ").append(record.summary.replace('\n', ' ')).append('\n')
                append(SEPARATOR).append('\n')
                append(record.trace)
            },
        )
        recordFiles().drop(MAX_RECORDS).forEach(File::delete)
    }

    // Newest first: the name is the record's time, and a later record never sorts below an earlier one.
    private fun recordFiles(): List<File> =
        directory().listFiles { file -> file.name.startsWith(PREFIX) }
            .orEmpty()
            .sortedWith(compareByDescending<File> { it.recordMillis() }.thenByDescending { it.name })

    private fun File.recordMillis(): Long =
        name.removePrefix(PREFIX).substringBefore('.').substringBefore('-').toLongOrNull() ?: 0L

    private fun read(file: File): CrashRecord? = runCatching {
        val text = file.readText()
        val head = text.substringBefore("\n$SEPARATOR\n").lines().associate {
            it.substringBefore(": ") to
                it.substringAfter(": ")
        }
        CrashRecord(
            at = Instant.parse(head.getValue("at")),
            kind = CrashKind.valueOf(head.getValue("kind")),
            summary = head.getValue("summary"),
            trace = text.substringAfter("\n$SEPARATOR\n", ""),
        )
    }.getOrNull()

    private fun marker(name: String) = File(directory().apply { mkdirs() }, name)

    private fun File.readInstant(): Instant? =
        takeIf(File::exists)?.readText()?.trim()?.toLongOrNull()?.let(Instant::ofEpochMilli)

    public companion object {

        public const val MAX_RECORDS: Int = 5

        /** 256 KB of text, about what the system's dump of one ANR's threads needs. */
        public const val MAX_TRACE_CHARS: Int = 256 * 1024

        private const val PREFIX = "crash-"
        private const val SEPARATOR = "---"
        private const val COLLECTED = "collected"
        private const val TOLD = "told"
    }
}
