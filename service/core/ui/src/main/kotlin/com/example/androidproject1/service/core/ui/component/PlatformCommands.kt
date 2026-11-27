package com.example.androidproject1.service.core.ui.component

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.example.androidproject1.service.core.ui.event.ExternalTarget
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.intents.AndroidSystemIntents
import com.example.androidproject1.service.intents.LaunchResult

/**
 * The commands that leave the app, resolved against a [Context].
 *
 * `Screen()` is the only caller: a feature names what it wants opened and never builds an intent.
 * The intents themselves are `:service:intents`' — this file turns a command's data into a
 * call and each function answers whether anything took it, and the caller tells the user when
 * nothing did.
 */
internal fun Context.share(command: UiCommand.Share): Boolean {
    val link = command.appPath?.let { "$packageName://${it.trimStart('/')}" }
    val text = listOfNotNull(command.text.takeIf { it.isNotBlank() }, link).joinToString(separator = "\n")
    return AndroidSystemIntents(this).shareText(text, command.subject).started()
}

/** Puts the text on the clipboard; true when the system confirms it and the caller need not. */
internal fun Context.copy(command: UiCommand.Copy): Boolean {
    val clip = ClipData.newPlainText(null, command.text)
    if (command.sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
    // Android 13 shows its own confirmation over every copy; a second one would be noise.
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

internal fun Context.openExternal(target: ExternalTarget): Boolean {
    val intents = AndroidSystemIntents(this)
    return when (target) {
        is ExternalTarget.Phone -> intents.dial(target.number)
        is ExternalTarget.Email -> intents.email(target.address, target.subject?.resolve(this), target.body)
        is ExternalTarget.Map -> intents.showOnMap(target.query)
        is ExternalTarget.CalendarEvent -> intents.addToCalendar(
            title = target.title,
            start = target.start,
            endInclusive = target.endInclusive,
            location = target.location,
        )
    }.started()
}

/** A Custom Tab over the app in [toolbarColor], or the system browser where there is none. */
internal fun Context.openBrowser(url: String, toolbarColor: Int): Boolean =
    AndroidSystemIntents(this).openUrl(url, toolbarColor).started()

private fun LaunchResult.started() = this == LaunchResult.Started

/**
 * Opens this app's page in system settings — where a permission refused for good can be granted.
 *
 * One implementation, used by both `Screen()`'s [UiCommand.OpenAppSettings] and
 * `:service:permission`'s rationale, so the two cannot drift. Public for that reason alone: a
 * feature sends the command.
 */
public fun Context.openAppSettings() {
    AndroidSystemIntents(this).openAppSettings()
}
