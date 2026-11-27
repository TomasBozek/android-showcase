package com.example.androidproject1.service.telemetry.test

import com.example.androidproject1.service.telemetry.CrashLog
import com.example.androidproject1.service.telemetry.CrashRecord
import java.io.File

/**
 * In-memory [CrashLog]: [records] are what the phone kept, newest first, and [takeNew] hands the
 * first of them out once, the way a launch is told about a crash once.
 */
public class FakeCrashLog(public val records: MutableList<CrashRecord> = mutableListOf()) : CrashLog {

    private var told = false

    override suspend fun collect(): Unit = Unit

    override suspend fun recent(): List<CrashRecord> = records.toList()

    override suspend fun takeNew(): CrashRecord? = records.firstOrNull()?.takeIf { !told }?.also { told = true }

    override suspend fun writeReport(into: File): File = into.apply { writeText(records.joinToString("\n\n")) }
}
