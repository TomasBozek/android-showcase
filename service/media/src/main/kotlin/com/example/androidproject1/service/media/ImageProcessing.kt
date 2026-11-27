package com.example.androidproject1.service.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * The non-UI half of handling a picture. A crop *screen* is a drawing and belongs to `:core:ui`;
 * everything it and the picker need to do to the pixels is here, where it is testable.
 */
public object ImageProcessing {

    /**
     * The square of [bitmap] at the centre of [rect]: as wide as [rect]'s shorter side, and always
     * inside the bitmap, so a rectangle that hangs over an edge is moved back in rather than
     * refused.
     */
    public fun squareCrop(bitmap: Bitmap, rect: Rect): Bitmap {
        val side = min(min(rect.width(), rect.height()), min(bitmap.width, bitmap.height)).coerceAtLeast(1)
        val left = (rect.centerX() - side / 2).coerceIn(0, bitmap.width - side)
        val top = (rect.centerY() - side / 2).coerceIn(0, bitmap.height - side)
        return Bitmap.createBitmap(bitmap, left, top, side, side)
    }

    /**
     * Writes the square of [file] that [area] names to [target] as a JPEG, at most [maxPx] a side,
     * and returns [target]. [area] is in fractions of the picture, as a crop screen reports
     * it, and goes through [squareCrop], so an area that hangs over an edge is moved back inside.
     * [file] is read as it is: the picker's files are already upright and without metadata.
     */
    public fun cropSquare(file: File, area: CropArea, maxPx: Int, target: File): File {
        require(maxPx > 0) { "maxPx must be positive" }
        val bitmap = BitmapFactory.decodeFile(file.path) ?: error("Not a picture: ${file.name}")
        val rect = Rect(
            (area.left * bitmap.width).toInt(),
            (area.top * bitmap.height).toInt(),
            ((area.left + area.width) * bitmap.width).toInt(),
            ((area.top + area.height) * bitmap.height).toInt(),
        )
        val cropped = squareCrop(bitmap, rect).cappedAt(maxPx)
        target.outputStream().use {
            check(cropped.compress(Bitmap.CompressFormat.JPEG, QUALITY, it)) { "Could not write the picture" }
        }
        return target
    }

    /**
     * Rewrites [file] as a JPEG no larger than [maxPx] on either side, and returns it.
     *
     * - **Orientation is applied, then dropped.** A phone stores a picture as the sensor saw it and
     *   a tag saying how to turn it; re-encoding without applying the tag puts it on its side.
     * - **Nothing else survives**: a re-encoded bitmap carries no EXIF, so the GPS position a photo
     *   was taken at does not go with it to a profile, an upload or a share.
     * - [square] takes the centre square first — what an avatar wants.
     *
     * The bytes are decoded at a reduced size when the picture is much larger than [maxPx], so a
     * 50-megapixel photo does not need 200 MB of memory to be made smaller.
     */
    public fun stripAndCap(file: File, maxPx: Int, square: Boolean = false): File {
        require(maxPx > 0) { "maxPx must be positive" }
        val orientation = ExifInterface(file.path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Not a picture: ${file.name}")
        val decoded = BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply {
                // The side that ends up mattering: a square is cut from the short one.
                inSampleSize = sampleSize(
                    if (square) min(bounds.outWidth, bounds.outHeight) else max(bounds.outWidth, bounds.outHeight),
                    maxPx,
                )
            },
        ) ?: error("Not a picture: ${file.name}")

        var bitmap = decoded.oriented(orientation)
        if (square) bitmap = squareCrop(bitmap, Rect(0, 0, bitmap.width, bitmap.height))
        bitmap = bitmap.cappedAt(maxPx)

        val out = File(file.parentFile, file.name + ".tmp")
        out.outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it)) { "Could not write the picture" }
        }
        check(out.renameTo(file) || (file.delete() && out.renameTo(file))) { "Could not replace ${file.name}" }
        return file
    }

    /** A power of two that still decodes to no less than [maxPx] on [side]. */
    private fun sampleSize(side: Int, maxPx: Int): Int {
        var sample = 1
        while (side / (sample * 2) >= maxPx) sample *= 2
        return sample
    }

    private fun Bitmap.oriented(orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Bitmap.cappedAt(maxPx: Int): Bitmap {
        val longSide = max(width, height)
        if (longSide <= maxPx) return this
        val scale = maxPx.toFloat() / longSide
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private const val QUALITY = 90
}
