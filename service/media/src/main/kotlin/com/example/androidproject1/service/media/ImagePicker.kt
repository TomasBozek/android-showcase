package com.example.androidproject1.service.media

import com.example.androidproject1.service.core.domain.result.Outcome
import java.io.File

/** Where a picture comes from. */
public enum class ImageSource {

    /** The system photo picker: it asks for no permission, and hands over the one picture the user chose. */
    Library,

    /** The camera app. The caller holds the `CAMERA` permission — `PermissionGate` — before asking. */
    Camera,
}

/**
 * A picture, as one `suspend` call.
 *
 * What comes back is **a file this app owns** — in its cache, whole, at most [MAX_PX] on a side and
 * with no metadata — never a URI that belongs to another app and may stop working. `null` is the
 * user backing out, which is not an error; a failure is a picture that could not be read.
 */
public interface ImagePicker {

    public suspend fun pick(source: ImageSource): Outcome<File?>

    public companion object {

        /** An avatar is shown small; 1024 is generous and keeps what is stored under a few hundred KB. */
        public const val MAX_PX: Int = 1024
    }
}
