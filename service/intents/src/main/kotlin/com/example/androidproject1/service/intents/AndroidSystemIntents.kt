package com.example.androidproject1.service.intents

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.example.androidproject1.service.files.AndroidFileStore
import com.example.androidproject1.service.files.FileStore
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * [SystemIntents] over a [Context] — an activity where there is one, so the command opens over the
 * screen it came from; any other context starts it in a task of its own.
 */
public class AndroidSystemIntents(
    private val context: Context,
    private val files: FileStore = AndroidFileStore(context),
) : SystemIntents {

    override fun shareText(text: String, subject: String?): LaunchResult {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        return launch(Intent.createChooser(send, null))
    }

    override fun shareFile(file: File, mimeType: String, title: String?): LaunchResult {
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, files.uriFor(file))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return launch(Intent.createChooser(send, title))
    }

    override fun openFile(file: File, mimeType: String): LaunchResult = launch(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(files.uriFor(file), mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )

    override fun openUrl(url: String, toolbarColor: Int?): LaunchResult {
        val tab = CustomTabsIntent.Builder().apply {
            toolbarColor?.let {
                setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(it).build())
            }
            setShowTitle(true)
        }.build()
        // `CustomTabsIntent` is a VIEW intent with extras, so a browser that ignores them just opens the page.
        tab.intent.data = Uri.parse(url)
        return launch(tab.intent)
    }

    override fun dial(number: String): LaunchResult = launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))

    /**
     * The subject and body go in the URI, not in extras: several mail apps read only the URI of a
     * `SENDTO`, and `mailto:` defines both as query fields.
     */
    override fun email(address: String, subject: String?, body: String?): LaunchResult {
        val fields = listOfNotNull(
            subject?.let { "subject=" + Uri.encode(it) },
            body?.let { "body=" + Uri.encode(it) },
        )
        val uri = buildString {
            append("mailto:").append(address)
            if (fields.isNotEmpty()) append('?').append(fields.joinToString("&"))
        }
        return launch(Intent(Intent.ACTION_SENDTO, Uri.parse(uri)))
    }

    // ACTION_SEND carries the attachment; the `mailto:` selector is what limits the chooser to mail
    // apps, since SENDTO cannot carry a stream.
    override fun emailFile(address: String, subject: String, body: String, file: File): LaunchResult {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
            .putExtra(Intent.EXTRA_STREAM, files.uriFor(file))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        return launch(Intent.createChooser(send, null))
    }

    override fun showOnMap(query: String): LaunchResult =
        launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}")))

    /**
     * An all-day event is midnight to midnight in UTC, whatever the device's zone, and ends at the
     * start of the day after the last one — that is what `CalendarContract` asks for.
     */
    override fun addToCalendar(
        title: String,
        start: LocalDate,
        endInclusive: LocalDate,
        location: String?,
    ): LaunchResult =
        launch(
            Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.utcMidnight())
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endInclusive.plusDays(1).utcMidnight())
                location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            },
        )

    override fun openAppSettings(): LaunchResult = launch(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )

    override fun openStorePage(): LaunchResult {
        val app = launch(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
        if (app == LaunchResult.Started) return app
        return launch(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"),
            ),
        )
    }

    private fun launch(intent: Intent): LaunchResult = try {
        // A context that is not an activity cannot start one without a task of its own.
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        LaunchResult.Started
    } catch (_: ActivityNotFoundException) {
        LaunchResult.NoActivity
    }

    private fun LocalDate.utcMidnight(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}
