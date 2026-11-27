package com.example.androidproject1.service.navigation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * How code outside a screen moves the user — the session, a server message, a notification,
 * a hold that lifts. It asks; the shell applies the ask through the [Navigator], after the session
 * gate, the way it applies a deep link. A screen navigates through its own [Navigator] instead.
 */
public interface AppNavigator {

    /** Puts [keys] on the stack: a single tab root is selected, anything else pushed. */
    public fun navigate(keys: List<NavKey>)

    /** Follows one of the app's own links, as if it had come from outside. */
    public fun open(deepLink: String)
}

/** One ask, as the shell receives it. */
public sealed interface AppNavigation {

    public data class Keys(val keys: List<NavKey>) : AppNavigation

    public data class Link(val deepLink: String) : AppNavigation
}

/**
 * The [AppNavigator] the app binds once: a buffered channel, so an ask made while nothing collects
 * — before the shell is up, across a rotation — arrives when it is, rather than being dropped.
 * Single-consumer by design: the shell is the one reader.
 */
public class AppNavigatorChannel : AppNavigator {

    private val channel = Channel<AppNavigation>(Channel.BUFFERED)

    /** What the shell collects. */
    public val requests: Flow<AppNavigation> = channel.receiveAsFlow()

    override fun navigate(keys: List<NavKey>) {
        channel.trySend(AppNavigation.Keys(keys))
    }

    override fun open(deepLink: String) {
        channel.trySend(AppNavigation.Link(deepLink))
    }
}

/**
 * The app navigator for `Screen()`, which interprets a `UiCommand.Navigate` — a message's action
 * that opens a route — through it. `null` where the shell provides none, and the command is then
 * dropped.
 */
public val LocalAppNavigator: ProvidableCompositionLocal<AppNavigator?> =
    staticCompositionLocalOf<AppNavigator?> { null }
