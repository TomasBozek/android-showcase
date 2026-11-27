package com.example.androidproject1

import com.example.androidproject1.debug.DebugMenu
import com.example.androidproject1.feature.devmenu.api.DevMenuDestination
import com.example.androidproject1.service.deeplink.DeepLinkTable
import com.example.androidproject1.service.deeplink.DeepLinkTarget
import com.example.androidproject1.service.deeplink.combineDeepLinks

/**
 * `<applicationId>://<door>/<segments…>` to the keys it lands on. Each feature that can be
 * linked to declares its own table in its api module — `<feature>DeepLinks`, a map from its first
 * segment — and is listed in [doors].
 */
object DeepLinks {

    /** The debug menu, which the `dev` and `staging` launcher shortcut opens: `<scheme>://devmenu`. */
    private val debugMenu: Map<String, DeepLinkTable> = mapOf(
        "devmenu" to DeepLinkTable { segments ->
            if (segments.isEmpty() && DebugMenu.ENABLED) {
                DeepLinkTarget(listOf(DevMenuDestination), needsSession = false)
            } else {
                null
            }
        },
    )

    private val doors: Map<String, DeepLinkTable> = combineDeepLinks(
        debugMenu,
    )

    fun parse(uri: String?): DeepLinkTarget? {
        val path = uri?.substringAfter(SCHEME_SEPARATOR, missingDelimiterValue = "")
            ?.split("/")
            ?.filter { it.isNotBlank() }
            ?: return null
        if (path.isEmpty()) return null
        return doors[path[0]]?.resolve(path.drop(1))
    }

    private const val SCHEME_SEPARATOR = "://"
}
