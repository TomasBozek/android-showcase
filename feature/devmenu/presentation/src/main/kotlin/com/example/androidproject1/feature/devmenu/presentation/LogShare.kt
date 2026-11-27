package com.example.androidproject1.feature.devmenu.presentation

/**
 * Hands the log file a `dev` or `staging` build keeps to the share sheet.
 *
 * A seam for the same reason [MessageReset] is one: the file is `:app`'s, bound where the debug
 * menu is, and the sheet is opened through the file store's `content://` uri, which this feature
 * has no business naming.
 */
interface LogShare {

    /** `false` when there is nothing to share yet — no line has been written. */
    suspend fun share(): Boolean
}
