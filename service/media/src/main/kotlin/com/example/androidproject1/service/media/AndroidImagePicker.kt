package com.example.androidproject1.service.media

import android.content.Context
import android.net.Uri
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.files.FilePurpose
import com.example.androidproject1.service.files.FileStore
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * [ImagePicker] over the system photo picker and the camera app, hosted by [ImageRequestActivity].
 *
 * What is returned is a JPEG in the cache, at most [maxPx] a side, upright and with no metadata —
 * whole unless [square], because which square an avatar keeps is the crop screen's
 * ([ImageProcessing.stripAndCap]). A picture from the library is *copied* first: its URI is read
 * access to somebody else's file, good only until this process dies. A capture is written to a
 * file of ours through the shared `FileProvider`, so there is nothing to copy.
 */
public class AndroidImagePicker(
    private val context: Context,
    private val files: FileStore,
    private val dispatchers: DispatcherProvider,
    private val maxPx: Int = ImagePicker.MAX_PX,
    private val square: Boolean = false,
) : ImagePicker {

    override suspend fun pick(source: ImageSource): Outcome<File?> {
        val target = files.file(FilePurpose.Cache, "picked-${System.nanoTime()}.jpg")
        val (id, answer) = PendingRequests.open()
        return try {
            val capture = if (source == ImageSource.Camera) files.uriFor(target) else null
            context.startActivity(ImageRequestActivity.intent(context, id, source, capture))
            val uri = answer.await()
            if (uri == null) {
                target.delete()
                Outcome.Success(null)
            } else {
                Outcome.Success(process(uri, source, target))
            }
        } catch (cancelled: CancellationException) {
            target.delete()
            throw cancelled
        } catch (failure: Throwable) {
            target.delete()
            Outcome.Failure(com.example.androidproject1.service.core.domain.error.UnexpectedError(cause = failure))
        } finally {
            PendingRequests.close(id)
        }
    }

    private suspend fun process(uri: Uri, source: ImageSource, target: File): File = withContext(dispatchers.io) {
        if (source == ImageSource.Library) {
            val input = context.contentResolver.openInputStream(uri) ?: error("Nothing to read at $uri")
            input.use { target.outputStream().use(it::copyTo) }
        }
        ImageProcessing.stripAndCap(target, maxPx, square)
    }
}
