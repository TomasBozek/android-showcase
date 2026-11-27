package com.example.androidproject1.service.intents.test

import com.example.androidproject1.service.intents.LaunchResult
import com.example.androidproject1.service.intents.SystemIntents
import java.io.File
import java.time.LocalDate

/** What a [FakeIntents] was asked to do. */
sealed interface IntentCall {
    data class ShareText(val text: String, val subject: String?) : IntentCall
    data class ShareFile(val file: File, val mimeType: String, val title: String?) : IntentCall
    data class OpenFile(val file: File, val mimeType: String) : IntentCall
    data class OpenUrl(val url: String, val toolbarColor: Int?) : IntentCall
    data class Dial(val number: String) : IntentCall
    data class Email(val address: String, val subject: String?, val body: String?) : IntentCall
    data class EmailFile(val address: String, val subject: String, val body: String, val file: File) : IntentCall
    data class ShowOnMap(val query: String) : IntentCall
    data class AddToCalendar(
        val title: String,
        val start: LocalDate,
        val endInclusive: LocalDate,
        val location: String?,
    ) :
        IntentCall
    data object OpenAppSettings : IntentCall
    data object OpenStorePage : IntentCall
}

/** A [SystemIntents] that records every command in [calls] and answers [result] — [LaunchResult.NoActivity] to script a phone with no mail app. */
class FakeIntents(var result: LaunchResult = LaunchResult.Started) : SystemIntents {

    val calls = mutableListOf<IntentCall>()

    private fun record(call: IntentCall): LaunchResult {
        calls += call
        return result
    }

    override fun shareText(text: String, subject: String?) = record(IntentCall.ShareText(text, subject))

    override fun shareFile(file: File, mimeType: String, title: String?) = record(
        IntentCall.ShareFile(file, mimeType, title),
    )

    override fun openFile(file: File, mimeType: String) = record(IntentCall.OpenFile(file, mimeType))

    override fun openUrl(url: String, toolbarColor: Int?) = record(IntentCall.OpenUrl(url, toolbarColor))

    override fun dial(number: String) = record(IntentCall.Dial(number))

    override fun email(address: String, subject: String?, body: String?) = record(
        IntentCall.Email(address, subject, body),
    )

    override fun emailFile(address: String, subject: String, body: String, file: File) =
        record(IntentCall.EmailFile(address, subject, body, file))

    override fun showOnMap(query: String) = record(IntentCall.ShowOnMap(query))

    override fun addToCalendar(title: String, start: LocalDate, endInclusive: LocalDate, location: String?) =
        record(IntentCall.AddToCalendar(title, start, endInclusive, location))

    override fun openAppSettings() = record(IntentCall.OpenAppSettings)

    override fun openStorePage() = record(IntentCall.OpenStorePage)
}
