package com.example.androidproject1.config

import kotlinx.serialization.Serializable

/**
 * What the server says about this app at start: the oldest build it still talks to, the
 * newest there is, and whether it is down for maintenance. Three fields and no flag map — a
 * generic switchboard waits for a behaviour that needs switching at runtime.
 *
 * Every field defaults to "nothing held", so a server that leaves one out holds nothing by it.
 */
@Serializable
data class AppConfig(
    val minVersionCode: Int = 0,
    val latestVersionCode: Int = 0,
    val maintenance: Boolean = false,
)

/** Why the whole app is held behind one screen, rather than any screen failing on its own. */
sealed interface AppHold {

    /** This build is older than the server's minimum: the store, or nothing. */
    data object UpdateRequired : AppHold

    /** The server is down on purpose, for everyone. */
    data object Maintenance : AppHold
}

/**
 * The hold [versionCode] is under, or `null`. An update outranks maintenance: a build too old to
 * talk to the server stays too old once the maintenance ends.
 */
fun AppConfig.holdFor(versionCode: Int): AppHold? = when {
    versionCode < minVersionCode -> AppHold.UpdateRequired
    maintenance -> AppHold.Maintenance
    else -> null
}
