package com.example.androidproject1.service.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The last few things the user did, for a crash report to carry: screen views and events,
 * newest last, the oldest dropped once there are [capacity].
 *
 * One per process, shared by [BreadcrumbAnalytics], which writes it, and [BreadcrumbErrorTracker],
 * which reads it. Thread-safe: a screen view arrives on the main thread and a report from anywhere.
 */
public class Breadcrumbs(private val capacity: Int = DEFAULT_CAPACITY) {

    private val crumbs = ArrayDeque<String>(capacity)

    @Synchronized
    public fun add(crumb: String) {
        if (crumbs.size == capacity) crumbs.removeFirst()
        crumbs.addLast(crumb)
    }

    /** Oldest first. */
    @Synchronized
    public fun snapshot(): List<String> = crumbs.toList()

    public companion object {
        /** Ten: enough to see the way to a screen and what was tapped there, short enough to read. */
        public const val DEFAULT_CAPACITY: Int = 10
    }
}

/** [Analytics] that leaves a breadcrumb for every screen view and event, then hands it to [delegate]. */
public class BreadcrumbAnalytics(
    private val delegate: Analytics,
    private val breadcrumbs: Breadcrumbs,
) : Analytics {

    override fun screen(id: String) {
        breadcrumbs.add("screen $id")
        delegate.screen(id)
    }

    override fun event(name: String, params: Map<String, Any?>) {
        breadcrumbs.add(if (params.isEmpty()) "event $name" else "event $name $params")
        delegate.event(name, params)
    }
}

/**
 * [ErrorTracker] whose every report carries the install id and the [breadcrumbs] that led to it,
 * appended to the message — a vendor console shows the message beside the stack, so this works
 * with any vendor, and with the log the default one writes to.
 *
 * The id is read once, on [scope], when the tracker is made: [recordNonFatal] cannot wait for
 * DataStore, so a report in the first moment of a first start says `install pending`.
 */
public class BreadcrumbErrorTracker(
    private val delegate: ErrorTracker,
    private val breadcrumbs: Breadcrumbs,
    installId: InstallId,
    scope: CoroutineScope,
) : ErrorTracker {

    @Volatile
    private var install: String? = null

    init {
        scope.launch { install = installId.get() }
    }

    override fun recordNonFatal(throwable: Throwable, message: String?) {
        val report = buildString {
            append(message ?: "Non-fatal")
            append("\ninstall ").append(install ?: "pending")
            val crumbs = breadcrumbs.snapshot()
            if (crumbs.isNotEmpty()) {
                append("\nbreadcrumbs, oldest first:")
                crumbs.forEach { append("\n  ").append(it) }
            }
        }
        delegate.recordNonFatal(throwable, report)
    }

    // Not a crumb of the ring: the HTTP log arrives here line by line and would push every screen
    // view out of it within one request. The vendor keeps its own breadcrumbs from these.
    override fun log(message: String): Unit = delegate.log(message)

    override fun setUser(id: String?): Unit = delegate.setUser(id)
}
