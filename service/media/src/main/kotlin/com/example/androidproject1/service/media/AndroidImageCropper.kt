package com.example.androidproject1.service.media

import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.core.domain.error.UnexpectedError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.files.FilePurpose
import com.example.androidproject1.service.files.FileStore
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** [ImageCropper] through [ImageProcessing.cropSquare], into the cache, off the main thread. */
public class AndroidImageCropper(
    private val files: FileStore,
    private val dispatchers: DispatcherProvider,
) : ImageCropper {

    override suspend fun crop(file: File, area: CropArea, sidePx: Int): Outcome<File> = withContext(dispatchers.io) {
        val target = files.file(FilePurpose.Cache, "cropped-${System.nanoTime()}.jpg")
        try {
            Outcome.Success(ImageProcessing.cropSquare(file, area, sidePx, target))
        } catch (cancelled: CancellationException) {
            target.delete()
            throw cancelled
        } catch (failure: Throwable) {
            target.delete()
            Outcome.Failure(UnexpectedError(cause = failure))
        }
    }
}
