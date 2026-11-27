package com.example.androidproject1.service.work.test

import com.example.androidproject1.service.work.Scheduler
import com.example.androidproject1.service.work.WorkHandler
import com.example.androidproject1.service.work.WorkResult
import com.example.androidproject1.service.work.WorkSpec
import kotlinx.coroutines.runBlocking

/**
 * A [Scheduler] that runs the work at once, on the caller's thread, with no delay, period or
 * constraint — a test asserts what the work did rather than waiting for a clock.
 *
 * [scheduled] and [cancelled] are what was asked for, in order; [results] is each name's last
 * answer. A handler nobody registered is a `Failure`, as it is on a device.
 */
class FakeScheduler(private val handlers: List<WorkHandler> = emptyList()) : Scheduler {

    val scheduled = mutableListOf<WorkSpec>()
    val cancelled = mutableListOf<String>()
    val results = mutableMapOf<String, WorkResult>()

    override fun schedule(spec: WorkSpec) {
        scheduled += spec
        val handler = handlers.firstOrNull { it.key == spec.handler }
        results[spec.name] = if (handler == null) WorkResult.Failure else runBlocking { handler.run(spec.input) }
    }

    override fun cancel(name: String) {
        cancelled += name
    }
}
