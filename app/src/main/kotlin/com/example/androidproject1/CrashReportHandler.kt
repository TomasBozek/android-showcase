package com.example.androidproject1

import com.example.androidproject1.service.core.ui.event.MessageActionHandler
import com.example.androidproject1.service.telemetry.CrashReports

/** The Send report on "the app closed unexpectedly": hands the crash records to a mail app. */
class CrashReportHandler(private val crashReports: CrashReports) : MessageActionHandler {

    override val key: String = KEY

    override suspend fun handle(argument: String) {
        crashReports.send()
    }

    companion object {

        const val KEY = "crash-report-send"
    }
}
