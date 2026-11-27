package com.example.androidproject1.service.telemetry.test

import com.example.androidproject1.service.telemetry.Analytics
import com.example.androidproject1.service.telemetry.ErrorTracker

/**
 * Records what would have been counted and reported, beside the two ports it fakes — one fake for
 * both, because a test that asserts an event usually wants the report that went with it too.
 *
 * [users] is a list rather than a latest-value, because the order matters: signing out has to send
 * `null`, and asserting only the final value would pass even if it never did.
 */
class FakeTelemetry :
    Analytics,
    ErrorTracker {

    data class Event(val name: String, val params: Map<String, Any?>)

    val screens = mutableListOf<String>()
    val events = mutableListOf<Event>()
    val users = mutableListOf<String?>()
    val recorded = mutableListOf<Throwable>()
    val logs = mutableListOf<String>()

    override fun screen(id: String) {
        screens += id
    }

    override fun event(name: String, params: Map<String, Any?>) {
        events += Event(name, params)
    }

    override fun recordNonFatal(throwable: Throwable, message: String?) {
        recorded += throwable
    }

    override fun log(message: String) {
        logs += message
    }

    override fun setUser(id: String?) {
        users += id
    }
}
