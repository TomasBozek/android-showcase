package com.example.androidproject1.service.deeplink

import androidx.navigation3.runtime.NavKey

/**
 * What one door's links open: handed the path segments after the door's own name, it
 * answers the target, or `null` when the rest is nobody's link — `weather/abc`, `product` with no
 * id. A feature's api module exports its doors as a map of name to table, so the feature that owns
 * a screen owns the links to it.
 */
public fun interface DeepLinkTable {
    public fun resolve(segments: List<String>): DeepLinkTarget?
}

/**
 * Where a link lands.
 *
 * @param keys the path below the app's start, the target last: what Up walks back through after a
 * cold start. The shell puts its start under it on a cold start and pushes the target alone on a
 * warm one.
 * @param needsSession `false` for a link followed by someone who cannot sign in — a password
 * reset — which lands on whichever flow is up, as it is, rather than waiting for a session.
 */
public data class DeepLinkTarget(val keys: List<NavKey>, val needsSession: Boolean = true) {

    init {
        require(keys.isNotEmpty()) { "A deep link lands somewhere" }
    }
}

/** A door that takes nothing after its name: the bare path is the link, anything under it is not. */
public fun bare(vararg keys: NavKey): DeepLinkTable {
    val target = DeepLinkTarget(keys.toList())
    return DeepLinkTable { segments -> target.takeIf { segments.isEmpty() } }
}

/** A door that takes one id after its name, parsed by [parse]; an id that does not parse is no link. */
public fun <T> withId(parse: (String) -> T?, target: (T) -> DeepLinkTarget): DeepLinkTable =
    DeepLinkTable { segments -> segments.singleOrNull()?.let(parse)?.let(target) }

/**
 * Every feature's doors as one table, refusing two features that claim the same door: the second
 * would silently win, and a link that opened one screen would start opening another.
 */
public fun combineDeepLinks(vararg tables: Map<String, DeepLinkTable>): Map<String, DeepLinkTable> {
    val combined = mutableMapOf<String, DeepLinkTable>()
    tables.forEach { table ->
        table.forEach { (door, links) ->
            check(combined.put(door, links) == null) { "Two features claim the deep link door '$door'" }
        }
    }
    return combined
}
