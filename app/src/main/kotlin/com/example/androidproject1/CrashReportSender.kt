package com.example.androidproject1

import com.example.androidproject1.service.files.FilePurpose
import com.example.androidproject1.service.files.FileStore
import com.example.androidproject1.service.intents.LaunchResult
import com.example.androidproject1.service.intents.SystemIntents
import com.example.androidproject1.service.telemetry.CrashLog
import com.example.androidproject1.service.telemetry.CrashReports

/**
 * [CrashReports] over the crash log: the records written to one file in the cache, which
 * the `FileProvider` serves, then handed to the share sheet or to a mail app addressed to
 * [supportEmail]. Here because only `:app` knows the support address and the build's version.
 */
class CrashReportSender(
    private val crashLog: CrashLog,
    private val files: FileStore,
    private val intents: SystemIntents,
    private val supportEmail: String,
    private val version: String,
) : CrashReports {

    override suspend fun share(): Boolean {
        val report = report() ?: return false
        return intents.shareFile(report, MIME_TYPE, null) == LaunchResult.Started
    }

    override suspend fun send(): Boolean {
        val report = report() ?: return false
        return intents.emailFile(
            address = supportEmail,
            subject = "Crash report, $version",
            body = "What I was doing when the app closed:\n\n",
            file = report,
        ) == LaunchResult.Started
    }

    private suspend fun report() = if (crashLog.recent().isEmpty()) {
        null
    } else {
        crashLog.writeReport(files.file(FilePurpose.Cache, REPORT_FILE))
    }

    private companion object {

        const val REPORT_FILE = "crash-report.txt"
        const val MIME_TYPE = "text/plain"
    }
}
