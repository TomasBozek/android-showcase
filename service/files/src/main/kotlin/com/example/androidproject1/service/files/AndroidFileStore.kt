package com.example.androidproject1.service.files

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [FileStore] over the app's own directories. The `FileProvider` is declared in this module's
 * manifest under `${applicationId}.files`, which is the authority [uriFor] asks for, so the three
 * flavors — whose ids differ by a suffix — each get their own.
 */
public class AndroidFileStore(private val context: Context) : FileStore {

    override fun file(purpose: FilePurpose, name: String): File {
        require(name.isNotEmpty() && name != "." && name != ".." && '/' !in name && '\\' !in name) {
            "A file name is one path segment, not \"$name\""
        }
        return File(directory(purpose).apply { mkdirs() }, name)
    }

    override fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    override suspend fun delete(file: File): Boolean = withContext(Dispatchers.IO) { !file.exists() || file.delete() }

    private fun directory(purpose: FilePurpose): File = when (purpose) {
        FilePurpose.Cache -> File(context.cacheDir, "files")
        FilePurpose.Documents -> File(context.filesDir, "documents")
        FilePurpose.Downloads -> File(context.filesDir, "downloads")
    }
}
