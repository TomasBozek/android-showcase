package com.example.androidproject1.service.intents

import java.io.File
import java.time.LocalDate

/** Whether anything took the command. [NoActivity] is an answer, not a crash: the caller says so. */
public enum class LaunchResult {
    Started,

    /** No installed app handles it — no mail client, no browser, no dialer. */
    NoActivity,
}

/**
 * What the platform can be asked to do, resolved in one place. A feature names what it wants and
 * never builds an intent; `Screen()` calls this for the commands a view model sends, and a
 * repository or a worker can inject it for the ones that are not on a screen.
 *
 * Whether anything handles a command is found by starting it and catching the failure, not by
 * asking the package manager first: from API 30 package visibility hides other apps from that
 * question unless the manifest lists every one in `<queries>`, so it would say "nothing" on every
 * phone.
 */
public interface SystemIntents {

    public fun shareText(text: String, subject: String? = null): LaunchResult

    /** [file] must be one of `FileStore`'s: another app is given a read grant on it and nothing else of ours. */
    public fun shareFile(file: File, mimeType: String, title: String? = null): LaunchResult

    /** Open [file] in whichever app views [mimeType]. Read-only. */
    public fun openFile(file: File, mimeType: String): LaunchResult

    /** A page in a Custom Tab over the app, coloured [toolbarColor] (an ARGB int), or the system browser where there is none. */
    public fun openUrl(url: String, toolbarColor: Int? = null): LaunchResult

    /** The dialler with [number] filled in. It does not call. */
    public fun dial(number: String): LaunchResult

    /** A new message in the user's mail app; [subject] and [body] are already resolved text. */
    public fun email(address: String, subject: String? = null, body: String? = null): LaunchResult

    /** The same, with [file] attached — offered to mail apps only, which may read it and nothing else. */
    public fun emailFile(address: String, subject: String, body: String, file: File): LaunchResult

    /** [query] on whichever map app the user has: a place, or an address. */
    public fun showOnMap(query: String): LaunchResult

    /** The calendar's "new event" form, all-day from [start] to [endInclusive]. */
    public fun addToCalendar(
        title: String,
        start: LocalDate,
        endInclusive: LocalDate,
        location: String? = null,
    ): LaunchResult

    /** This app's page in system settings — where a permission refused for good can be granted. */
    public fun openAppSettings(): LaunchResult

    /** This app's page in its store: the store app where there is one, otherwise the web page. */
    public fun openStorePage(): LaunchResult
}
