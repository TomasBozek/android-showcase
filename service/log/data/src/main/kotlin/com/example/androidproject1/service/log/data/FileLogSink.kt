package com.example.androidproject1.service.log.data

import java.io.File
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** Somewhere besides logcat a log line goes; [AndroidLogger] writes to one when it is given one. */
public interface LogSink {

    /** [level] is logcat's letter — `D`, `W`, `E`. */
    public fun write(level: Char, tag: String, message: String, throwable: Throwable?)
}

/**
 * A [LogSink] that appends to a file, for a tester to share from the debug menu.
 *
 * **Bounded.** Past [maxBytes] the file becomes `<name>.1`, replacing the one before it, and a new
 * file starts — so the log never holds more than twice the cap, and [snapshot] hands over both.
 *
 * **Off the caller's thread.** Every write is queued on [executor], one thread by default, so a
 * log call on the main thread is not a disk write there (StrictMode would say so) and lines keep
 * their order. [file] is asked for on that thread too, since making its directory touches disk.
 */
public class FileLogSink(
    file: () -> File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val executor: Executor = Executors.newSingleThreadExecutor(::daemon),
    private val clock: Clock = Clock.systemUTC(),
) : LogSink {

    private val file by lazy(file)

    private val previous: File get() = File(file.parentFile, file.name + ".1")

    override fun write(level: Char, tag: String, message: String, throwable: Throwable?) {
        val time = Instant.now(clock)
        executor.execute {
            val line = buildString {
                append(time).append(' ').append(level).append('/').append(tag).append(": ").append(message).append('\n')
                throwable?.let { append(it.stackTraceToString()) }
            }
            val bytes = line.toByteArray()
            if (file.length() > 0 && file.length() + bytes.size > maxBytes) rotate()
            file.appendBytes(bytes)
        }
    }

    /**
     * Everything the log holds, oldest first, copied to [into] after every write already queued —
     * so the copy can be shared while the log goes on. Blocks: call it off the main thread.
     */
    public fun snapshot(into: File): File {
        val done = CountDownLatch(1)
        executor.execute {
            into.writeBytes(ByteArray(0))
            listOf(previous, file).filter(File::exists).forEach { into.appendBytes(it.readBytes()) }
            done.countDown()
        }
        done.await()
        return into
    }

    private fun rotate() {
        previous.delete()
        file.renameTo(previous)
    }

    private companion object {
        fun daemon(work: Runnable) = Thread(work, "FileLogSink").apply { isDaemon = true }

        /** Half a megabyte: days of a tester's session, and small enough to attach to a mail. */
        const val DEFAULT_MAX_BYTES = 512L * 1024
    }
}
