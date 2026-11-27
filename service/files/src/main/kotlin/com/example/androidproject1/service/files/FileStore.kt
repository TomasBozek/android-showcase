package com.example.androidproject1.service.files

import android.net.Uri
import java.io.File

/**
 * Where a file lives is decided by what it is for, so no feature picks a directory of its own.
 *
 * - [Cache]: what can be made again. The system may clear it whenever it needs the space.
 * - [Documents]: what the user made and the app keeps — a photo, an export.
 * - [Downloads]: what came over the network and was worth keeping; large, and never cleared.
 *
 * All three are private to the app. A file another app is meant to see goes through [FileStore.uriFor].
 */
public enum class FilePurpose { Cache, Documents, Downloads }

public interface FileStore {

    /**
     * The file called [name] for [purpose], in a directory that exists. [name] is one path segment:
     * a name that could step out of the directory (`..`, a separator) is refused, so a name taken
     * from a server or a user cannot write anywhere else.
     */
    public fun file(purpose: FilePurpose, name: String): File

    /** A `content://` URI another app can read [file] through, for as long as the grant lasts. */
    public fun uriFor(file: File): Uri

    /** Deletes [file] off the main thread; `true` when there is nothing left of it. */
    public suspend fun delete(file: File): Boolean
}
