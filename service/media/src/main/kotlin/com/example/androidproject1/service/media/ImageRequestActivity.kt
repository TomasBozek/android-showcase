package com.example.androidproject1.service.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Waiting requests, by id. [AndroidImagePicker] parks a caller here and starts
 * [ImageRequestActivity]; the activity answers, once, and the caller resumes.
 *
 * In memory: a request does not survive the process, and there is nobody left to answer.
 */
internal object PendingRequests {

    private val next = AtomicInteger()
    private val waiting = ConcurrentHashMap<Int, CompletableDeferred<Uri?>>()

    fun open(): Pair<Int, CompletableDeferred<Uri?>> {
        val id = next.incrementAndGet()
        return id to CompletableDeferred<Uri?>().also { waiting[id] = it }
    }

    fun answer(id: Int, uri: Uri?) {
        waiting.remove(id)?.complete(uri)
    }

    fun close(id: Int) {
        waiting.remove(id)
    }
}

/**
 * Runs the photo picker or the camera on behalf of [AndroidImagePicker] and reports where the
 * picture is, or `null` when the user backed out. Invisible: it exists to hold the result contracts.
 *
 * It is finished the moment it has answered. If the process is killed while another app is in
 * front, the recreated activity has nobody waiting for it and simply finishes.
 */
internal class ImageRequestActivity : ComponentActivity() {

    private var id = 0
    private var capture: Uri? = null

    private val library = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { answer(it) }
    private val camera = registerForActivityResult(CaptureToUri) { saved -> answer(capture.takeIf { saved }) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getIntExtra(EXTRA_ID, 0)
        capture = intent.data
        // Only the first time: after a recreation the launcher's own result is already on its way.
        if (savedInstanceState != null) return
        when (intent.getStringExtra(EXTRA_SOURCE)) {
            ImageSource.Camera.name -> camera.launch(requireNotNull(capture) { "a capture needs somewhere to go" })
            else -> library.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun answer(uri: Uri?) {
        PendingRequests.answer(id, uri)
        finish()
    }

    /** Backing out of a screen that never appeared, or the system ending it: an answer of "nothing". */
    override fun onDestroy() {
        if (isFinishing) PendingRequests.answer(id, null)
        super.onDestroy()
    }

    companion object {

        private const val EXTRA_ID = "media.id"
        private const val EXTRA_SOURCE = "media.source"

        fun intent(context: Context, id: Int, source: ImageSource, capture: Uri?): Intent =
            Intent(context, ImageRequestActivity::class.java)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_SOURCE, source.name)
                .setData(capture)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/**
 * `TakePicture`, plus the grant the camera app needs for the file it is handed.
 *
 * The stock contract puts the URI in `EXTRA_OUTPUT` and flags nothing, so another app cannot open
 * it — and the symptom is a zero-byte file rather than an error, which is the kind of thing that
 * reaches a release. `createIntent` is open for exactly this.
 */
private object CaptureToUri : ActivityResultContracts.TakePicture() {

    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
}
