package com.example.androidproject1.service.log.data

import android.util.Log
import com.example.androidproject1.service.log.domain.Logger

/**
 * [Logger] backed by `android.util.Log`.
 *
 * @param minLevel the lowest level that reaches logcat. Bind this with [Log.WARN] or higher in
 * release builds — this module cannot read the app's `BuildConfig`, so the decision is the
 * consuming app's to make in its Koin module. See `coreModule`.
 * @param sink where a line goes besides logcat — the file a `dev` or `staging` build keeps
 * — or `null`, which is `prod`: nothing on disk. It gets exactly what passes [minLevel].
 */
public class AndroidLogger(
    private val tag: String = "App",
    private val minLevel: Int = Log.DEBUG,
    private val sink: LogSink? = null,
) : Logger {

    override fun withTag(tag: String): Logger = AndroidLogger(tag = tag, minLevel = minLevel, sink = sink)

    override fun d(throwable: Throwable?, message: () -> String) {
        log(Log.DEBUG, throwable, message)
    }

    override fun w(throwable: Throwable?, message: () -> String) {
        log(Log.WARN, throwable, message)
    }

    override fun e(throwable: Throwable?, message: () -> String) {
        log(Log.ERROR, throwable, message)
    }

    // The message lambda is only invoked once the level passes, so a filtered-out call costs
    // nothing beyond the comparison — string building included.
    private fun log(level: Int, throwable: Throwable?, message: () -> String) {
        if (level < minLevel) return
        val text = message()
        Log.println(level, tag, text)
        throwable?.let { Log.println(level, tag, Log.getStackTraceString(it)) }
        sink?.write(letter(level), tag, text, throwable)
    }

    private fun letter(level: Int): Char = when (level) {
        Log.ERROR -> 'E'
        Log.WARN -> 'W'
        else -> 'D'
    }
}
