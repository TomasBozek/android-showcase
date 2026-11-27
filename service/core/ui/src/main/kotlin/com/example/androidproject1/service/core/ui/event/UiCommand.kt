package com.example.androidproject1.service.core.ui.event

import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.service.core.ui.text.UiText
import java.time.LocalDate

/**
 * One-off platform effects any screen may request. Interpreted centrally, so features never touch
 * a Context.
 */
public sealed interface UiCommand {

    public data object NavigateBack : UiCommand

    public data object CloseApp : UiCommand

    /**
     * The one way to acknowledge something in passing — a toast is this with no action.
     * `Screen()` posts it to the app's outlet, `AppMessages`, so it outlives the screen, and
     * the chrome draws it, so it wears the theme and needs no Scaffold of its own.
     *
     * @param id identifies this snackbar when its action comes back as
     * [SystemEvent.SnackbarAction]. Every command is plain data — pressing the action raises an
     * event like any other button, rather than invoking a lambda the command carried.
     * @param actionLabel shows an action button when non-null — a snackbar; without one it is a
     * toast in its [tone].
     */
    public data class ShowSnackbar(
        val id: String,
        val message: UiText,
        val actionLabel: UiText? = null,
        val withDismissAction: Boolean = false,
        val tone: ToastTone = ToastTone.Neutral,
    ) : UiCommand

    /**
     * A web page, shown in a Custom Tab in the theme's colours so the user stays in the app; the
     * system browser when no browser supports Custom Tabs.
     */
    public data class OpenBrowser(val url: String) : UiCommand

    /**
     * Opens a route through the app navigator — what a message's action does to take the
     * user somewhere, since an action is data. A screen's own navigation stays its `Navigator`'s.
     */
    public data class Navigate(val keys: List<NavKey>) : UiCommand

    public data object OpenAppSettings : UiCommand

    /**
     * The system share sheet.
     *
     * @param appPath a path in this app's own link scheme — `product/42` — which the shell turns
     * into a link and appends to [text]. The scheme is the application id, which only the
     * shell can read, so a feature names the door and never the scheme.
     */
    public data class Share(
        val text: String,
        val subject: String? = null,
        val appPath: String? = null,
    ) : UiCommand

    /**
     * The clipboard. Android 13 and up confirm a copy themselves; below that the shell shows a
     * snackbar with no action, which is this system's toast.
     *
     * @param sensitive keeps the text out of the clipboard preview on Android 13 and up.
     */
    public data class Copy(val text: String, val sensitive: Boolean = false) : UiCommand

    /** Another app: the dialler, a mail client, a map, the calendar. */
    public data class OpenExternal(val target: ExternalTarget) : UiCommand
}

/**
 * What [UiCommand.OpenExternal] opens. Each is resolved by `Screen()` into the one intent the
 * platform defines for it; when no app on the device can take it, the user is told so in a
 * snackbar rather than nothing happening.
 */
public sealed interface ExternalTarget {

    /** `tel:` — the dialler with the number filled in; nothing is called. */
    public data class Phone(val number: String) : ExternalTarget

    /**
     * `mailto:`, with the subject and the body filled in. The subject is [UiText] because it is
     * copy and gets translated; the body is whatever the caller needs a reply to know.
     */
    public data class Email(val address: String, val subject: UiText? = null, val body: String? = null) : ExternalTarget

    /** `geo:0,0?q=` — a place by name, for whatever map app is installed. */
    public data class Map(val query: String) : ExternalTarget

    /** A calendar insert, all day from [start] through [endInclusive], saved only by the user. */
    public data class CalendarEvent(
        val title: String,
        val start: LocalDate,
        val endInclusive: LocalDate,
        val location: String? = null,
    ) : ExternalTarget
}
