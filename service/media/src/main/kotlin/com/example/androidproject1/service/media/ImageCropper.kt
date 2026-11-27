package com.example.androidproject1.service.media

import com.example.androidproject1.service.core.domain.result.Outcome
import java.io.File

/**
 * Which part of a picture to keep, in fractions of its width and height: [left] and [top] are the
 * corner, [width] and [height] the extent over each — what a crop screen reports, whatever size it
 * drew the picture at.
 */
public data class CropArea(val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * A square cut from a picture the user framed, as one `suspend` call — the other half of a
 * crop screen, which only says where.
 *
 * What comes back is a new file this app owns, in its cache, square and at most [sidePx] a side;
 * the picture it was cut from is left as it was. A failure is a picture that could not be read.
 */
public interface ImageCropper {

    public suspend fun crop(file: File, area: CropArea, sidePx: Int = AVATAR_PX): Outcome<File>

    public companion object {

        /** An avatar's side: a profile shows it at a few dozen dp, and 512 is sharp at any of them. */
        public const val AVATAR_PX: Int = 512
    }
}
